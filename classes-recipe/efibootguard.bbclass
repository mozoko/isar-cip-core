#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2024 - 2026
#
# Authors:
#  Quirin Gylstorff <quirin.gylstorff@siemens.com>
#
# SPDX-License-Identifier: MIT
#

require recipes-core/images/swupdate-efibootguard.inc
do_warn_custom_inc() {
    bbwarn "Please migrate from \"inherit efibootguard\" to \"recipes-core/images/swupdate-efibootguard.inc\""
}
addtask warn_custom_inc before do_unpack
