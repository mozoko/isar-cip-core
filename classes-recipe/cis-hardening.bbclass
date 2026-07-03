#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2026
#
# Authors:
#  Felix Moessbauer <felix.moessbauer@siemens.com>
#
# SPDX-License-Identifier: MIT

# The config file is generated during install, hence
# change permissions after all installs are settled
apply_sshd_hardening[weight] = "1"
apply_sshd_hardening() {
    CIS_SSHD=/usr/share/cip-cis-rules/configuration/cis-5.2.1.sh
    if [ -f "${ROOTFSDIR}/${CIS_SSHD}" ] && [ -f "${ROOTFSDIR}/usr/share/openssh/sshd_config" ]; then
        sudo chroot "${ROOTFSDIR}" ${CIS_SSHD}
    fi
}
ROOTFS_POSTPROCESS_COMMAND += "apply_sshd_hardening"

def assert_hardening_kernel_option(d, option, state, error):
    """
    Fail with the given error if a kernel hardening option is set to the given
    state in the hardening config file.
    """
    import os

    hardening_cfg = "{}/{}".format(d.getVar("DEPLOY_DIR_IMAGE"), d.getVar("CIP_CIS_HARDENING_KCONFIG"))
    if not os.path.isfile(hardening_cfg):
        bb.fatal("CIS kernel hardening config {} not found.".format(hardening_cfg))
    with open(hardening_cfg) as f:
        lines = {line.strip() for line in f}

    if state == "yes":
        has_option = "CONFIG_{}=y".format(option) in lines
    elif state == "not set":
        has_option = "# CONFIG_{} is not set".format(option) in lines
    else:
        bb.fatal("Unsupported CIS kernel hardening option state: {}".format(state))

    if has_option:
        bb.fatal(error)

python detect_squashfs_conflict() {
    assert_hardening_kernel_option(d, "SQUASHFS", "not set",
        "CIS kernel hardening disables squashfs, but squashfs is selected as image fs. "
        "Consider disabling the hardening option for squashfs or using an alternative fs.")
}

do_image_squashfs[prefuncs] += " detect_squashfs_conflict "
do_image_squashfs[depends] += " cip-cis-rules-config:do_deploy_hardening "
