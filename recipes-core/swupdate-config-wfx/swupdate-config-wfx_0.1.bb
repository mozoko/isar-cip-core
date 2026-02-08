#
# CIP Core, generic profile
#
# Copyright (c) Toshiba Corporation, 2026
#
# Authors:
#  Adithya Balakumar <adithya.balakumar@toshiba-tsip.com>
#
# SPDX-License-Identifier: MIT
#

inherit dpkg-raw

PROVIDES := "${PN}"
DEBIAN_PROVIDES := "${PN}"

PN .= "-${MACHINE}"
SURICATTA_WFX_CONF = "${@ 'suricatta_wfx_onboarding.conf' if 'wfx-onboard' in d.getVar('OVERRIDES') else 'suricatta_wfx.conf' }"
SRC_URI:append = " ${@ 'file://' + d.getVar('SURICATTA_WFX_CONF') + '.tmpl'}"
SRC_URI:append:wfx-onboard = " file://swupdate_override.conf"

TEMPLATE_FILES += " ${@ d.getVar('SURICATTA_WFX_CONF') + '.tmpl'}"
TEMPLATE_VARS += " WFX_URL"

do_install[cleandirs] = "${D}/etc/swupdate/conf.d/"
do_install() {
    install -d ${D}/etc/swupdate/conf.d
    install -m 0644 ${WORKDIR}/${SURICATTA_WFX_CONF} ${D}/etc/swupdate/conf.d/suricatta_wfx.conf
}

do_install:append:wfx-onboard() {
    install -d ${D}/etc/systemd/system/swupdate.service.d
    install -m 0644 ${WORKDIR}/swupdate_override.conf ${D}/etc/systemd/system/swupdate.service.d
}
