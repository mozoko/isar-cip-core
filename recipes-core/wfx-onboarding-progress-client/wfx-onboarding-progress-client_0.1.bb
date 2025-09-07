#
# CIP Core, generic profile
#
# Copyright (c) Toshiba Corporation 2025
#
# Authors:
#  Adithya Balakumar <adithya.balakumar@toshiba-tsip.com>
#
# SPDX-License-Identifier: MIT
#

inherit dpkg

SRC_URI += "git://gitlab.com/cip-playground/cip-wfx-onboarding-client-plugin.git;protocol=https;destsuffix=cip-wfx-onboarding-client-plugin;name=cip-wfx-onboarding-client-plugin;branch=master"
SRCREV_cip-wfx-onboarding-client-plugin ?= "250702531bdbfb1a6274d91d3d3b56a33e80e1fc"

WFX_ONBOARDING_PROGRESS_CLIENT = "cip-wfx-onboarding-client-plugin/onboarding-progress-client"

SRC_URI += "file://postinst"

do_prepare_build[cleandirs] += "${S}"
do_prepare_build() {
    cp -r ${WORKDIR}/${WFX_ONBOARDING_PROGRESS_CLIENT}/* ${S}/

    deb_add_changelog
}
