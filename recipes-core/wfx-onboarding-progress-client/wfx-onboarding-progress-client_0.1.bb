#
# CIP Core, generic profile
#
# Copyright (c) Toshiba Corporation 2026
#
# Authors:
#  Adithya Balakumar <adithya.balakumar@toshiba-tsip.com>
#
# SPDX-License-Identifier: MIT
#

inherit dpkg

SRC_URI += "git://gitlab.com/cip-playground/cip-wfx-onboarding-client-plugin.git;protocol=https;destsuffix=cip-wfx-onboarding-client-plugin;name=cip-wfx-onboarding-client-plugin;branch=master"
SRCREV_cip-wfx-onboarding-client-plugin ?= "5732bce8ae2d1d744827d42e00cc13ba251bf75c"

WFX_ONBOARDING_PROGRESS_CLIENT = "cip-wfx-onboarding-client-plugin/onboarding-progress-client"

SRC_URI += "file://postinst"

do_prepare_build[cleandirs] += "${S}"
do_prepare_build() {
    cp -r ${WORKDIR}/${WFX_ONBOARDING_PROGRESS_CLIENT}/* ${S}/

    deb_add_changelog
}
