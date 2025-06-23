#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2024
#
# Authors:
#  Quirin Gylstorff <quirin.gylstorff@siemens.com>
#
# SPDX-License-Identifier: MIT
#

inherit dpkg-raw

PROVIDES := "${PN}"
DEBIAN_PROVIDES := "${PN}"

PN .= "-${MACHINE}"

SRC_URI = "file://swupdate.cfg.tmpl \
           file://hwrevision.tmpl \
           file://suricatta \
           file://swupdate_execstartpre.conf \
           file://add-swupdate-http-headers.sh"

MACHINE_HW_VERSION ??= "cip-core-1.0"
TEMPLATE_FILES += "hwrevision.tmpl swupdate.cfg.tmpl"
TEMPLATE_VARS += "MACHINE MACHINE_HW_VERSION WFX_URL"

do_install[cleandirs] = "${D}/etc/"
do_install() {
    install -v -m 644 "${WORKDIR}"/swupdate.cfg "${D}"/etc/
    if [ -n "${MACHINE_HW_VERSION}" ]; then
        install -v -m 644 "${WORKDIR}"/hwrevision "${D}"/etc/
    fi

    # start swupdate service in suricatta mode
	install -d ${D}/etc/swupdate/conf.d
	install -m 0644 ${WORKDIR}/suricatta ${D}/etc/swupdate/conf.d

    install -d ${D}/etc/systemd/system/swupdate.service.d
    install -m 0644 ${WORKDIR}/swupdate_execstartpre.conf ${D}/etc/systemd/system/swupdate.service.d
    install -m 0755 ${WORKDIR}/add-swupdate-http-headers.sh ${D}/etc/systemd/system/swupdate.service.d
    
}
