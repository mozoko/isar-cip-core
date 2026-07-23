# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2026
#
# Authors:
#  Quirin Gylstorff <quirin.gylstorff@siemens.com>
#
# SPDX-License-Identifier: MIT
#
inherit dpkg

MAINTAINER = "Christoph Steiger <christoph.steiger@siemens.com>"
DESCRIPTION = "Configuration files for CIS hardening"

PROVIDES := "${PN}"
DEBIAN_PROVIDES := "${PN}"

DEPENDS = "cip-cis-rules cis-hardening"
DEBIAN_BUILD_DEPENDS = "cip-cis-rules, cis-hardening, python3, python3-kconfiglib, python3-newt, python3-jsonschema"

PN .= "-${MACHINE}"

S = "${WORKDIR}/${BPN}"

DEPLOYDIR_HARDENING = "${WORKDIR}/deploy-hardening"
SSTATETASKS += "do_deploy_hardening"

HARDENING_CFG ?= ""
HARDENING_CFG_NAME = "${@ d.getVar('HARDENING_CFG') if d.getVar('HARDENING_CFG') else 'config.json'}"

HARDENING_CFG:ci = "ci.config.json"

SRC_URI += " \
    file://scripts;subdir=${S} \
    file://rules.tmpl \
    file://postinst \
    "
SRC_URI += "${@ 'file://' + d.getVar('HARDENING_CFG') if d.getVar('HARDENING_CFG') else ''}"

TEMPLATE_VARS += "HARDENING_CFG_NAME"
TEMPLATE_FILES += "rules.tmpl"

do_deploy_hardening[cleandirs] += "${DEPLOYDIR_HARDENING}"
do_deploy_hardening[sstate-inputdirs] = "${DEPLOYDIR_HARDENING}"
do_deploy_hardening[sstate-outputdirs] = "${DEPLOY_DIR_IMAGE}"
do_deploy_hardening() {
    if [ -f "${S}/${HARDENING_CFG_NAME}" ]; then
        cp "${S}/${HARDENING_CFG_NAME}" "${DEPLOYDIR_HARDENING}/${CIP_CIS_HARDENING_CONFIG}"
    fi
    if [ -f "${S}/hardening.cfg" ]; then
        cp "${S}/hardening.cfg" "${DEPLOYDIR_HARDENING}/${CIP_CIS_HARDENING_KCONFIG}"
    fi
}

python do_deploy_hardening_setscene () {
    sstate_setscene(d)
}
addtask deploy_hardening_setscene

addtask deploy_hardening after do_dpkg_build before do_deploy_deb

do_check_config() {
    if [ ! -s "${S}/${HARDENING_CFG_NAME}" ]; then
        bbfatal "Hardening configuration is missing. Please run 'bitbake cip-cis-rules-config -c menuconfig' and include the generated ${WORKDIR}/${HARDENING_CFG_NAME} in your SRC_URI and HARDENING_CFG_NAME."
    fi
}
addtask check_config after do_prepare_build before do_dpkg_build do_generate_kernel_config

do_generate_kernel_config() {
    if [ -f "${S}/${HARDENING_CFG_NAME}" ]; then
        "${S}/scripts/gen-kernel-config.py" --config "${S}/${HARDENING_CFG_NAME}" --output "${S}/hardening.cfg"
    fi
}
addtask generate_kernel_config before do_dpkg_build after do_prepare_build

do_prepare_build() {
    deb_debianize

    cat <<EOF > "${S}/debian/${BPN}.install"
${HARDENING_CFG_NAME} /usr/share/cip-cis-rules/
hardening.cfg /usr/share/cip-cis-rules/
EOF
    if [ -f "${WORKDIR}/${HARDENING_CFG_NAME}" ]; then
        install "${WORKDIR}/${HARDENING_CFG_NAME}" "${S}/${HARDENING_CFG_NAME}"
    fi
}

do_menuconfig[nostamp] = "1"
do_menuconfig[dirs] = "${DEVSHELL_STARTDIR}"
do_menuconfig[network] = "${TASK_USE_SUDO}"
do_menuconfig[depends] = "cip-cis-rules:do_deploy_deb cis-hardening:do_deploy_deb"
python do_menuconfig() {
    isar_export_proxies(d)
    isar_export_ccache(d)
    isar_export_build_settings(d)

    bb.build.exec_func('devshell_chroot_prepare', d)

    schroot = d.getVar('SBUILD_CHROOT')
    pkg_arch = d.getVar('PACKAGE_ARCH')
    build_arch = d.getVar('BUILD_ARCH')
    pp_pps = os.path.join(d.getVar('PP'), d.getVar('PPS'))
    srcdir = d.getVar('S')
    config = d.getVar('HARDENING_CFG_NAME')
    configfile = os.path.join(srcdir, config)

    try:
        mtime = os.path.getmtime(configfile)
    except OSError:
        mtime = 0

    install_deps = ":" if d.getVar('BB_CURRENTTASK') == "devshell_nodeps" else f"mk-build-deps -i \
        --host-arch {pkg_arch} --build-arch {build_arch}  \
        -t \"apt-get -y -q -o Debug::pkgProblemResolver=yes --no-install-recommends --allow-downgrades\" \
        debian/control"

    termcmd = "cd {0}; \
        apt-get -y -q update -o Dir::Etc::SourceList=\"sources.list.d/isar-apt.list\" -o Dir::Etc::SourceParts=\"-\" -o APT::Get::List-Cleanup=\"0\"; \
        apt-get -y upgrade; \
        {1}; \
        if [ -n \"$PATH_PREPEND\" ]; then export PATH=$PATH_PREPEND:$PATH; fi; \
        {0}/scripts/cis-hardening-to-kconfig.py /opt/cis-hardening/versions/default > {0}/Kconfig; \
        {0}/scripts/select-hardenings.py --output {0}/{2} {0}/Kconfig".format(pp_pps, install_deps, config)

    if d.getVar('ISAR_CHROOT_MODE') == 'unshare':
        mounts = d.getVar('SCHROOT_MOUNTS')
        mounts += ' {}:/home/builder/{}'.format(d.getVar('WORKDIR'), d.getVar('BPN'))

        if bb.utils.to_boolean(d.getVar('USE_CCACHE')):
            bb.build.exec_func('dpkg_prepare_unshare_ccache', d)
            mounts += ' {}:/ccache'.format(d.getVar('CCACHE_DIR'))

        termcmd = """{0} \
sh -c "{1};cp /etc/resolv.conf {2}/etc;chroot {2} sh -c '{3}'"
""".format(
        run_privileged_cmd(d),
        insert_isar_mounts(d, d.getVar('DEVSHELL_UNSHARE_ROOTFS'), mounts),
        d.getVar('DEVSHELL_UNSHARE_ROOTFS'),
        termcmd.replace('"', "\\\""))
    else:
        termcmd = "schroot -d / -c {0} -u root -- sh -c '{1}'".format(schroot, termcmd)
    oe_terminal(termcmd, "Generate Kconfig", d)

    bb.build.exec_func('devshell_chroot_finalize', d)

    try:
        newmtime = os.path.getmtime(configfile)
    except OSError:
        newmtime = 0

    if newmtime > mtime:
        bb.note("Configuration changed, recompile will be forced")
        bb.build.write_taint('do_generate_kernel_config', d)
        bb.build.write_taint('do_deploy_hardening', d)
        bb.build.write_taint('do_dpkg_source', d)
        bb.build.write_taint('do_dpkg_build', d)

}
addtask menuconfig after do_prepare_build do_local_isarapt
