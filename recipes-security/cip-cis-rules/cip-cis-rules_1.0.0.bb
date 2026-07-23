# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2026
#
# Authors:
#  Quirin Gylstorff <quirin.gylstorff@siemens.com>
#
# SPDX-License-Identifier: MIT
#
DESCRIPTION = "CIS rule overrides for CIP based images"
MAINTAINER = "Felix Moessbauer <felix.moessbauer@siemens.com>"

SRC_URI = "git://gitlab.com/cip-playground/cip-cis-rules.git;protocol=https;branch=cis-debian-13;destsuffix=${P}"
SRCREV = "094acbe4926840f924c35b0526ee4f316984f891"

DPKG_ARCH = "all"
PROVIDES = "${BPN}"

inherit dpkg

do_prepare_build() {
    deb_add_changelog
}
