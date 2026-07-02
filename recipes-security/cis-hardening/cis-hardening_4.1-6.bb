#
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

DPKG_ARCH = "all"

SRC_URI = "git://github.com/ovh/debian-cis.git;protocol=https;destsuffix=${P};nobranch=1"
SRCREV = "efbd5b35ba32226e8950d52bd3f3444c702bbd84"
