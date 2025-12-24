#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2022-2025
#
# Authors:
#  Jan Kiszka <jan.kiszka@siemens.com>
#
# SPDX-License-Identifier: MIT
#

require u-boot-common-${PV}.inc
require u-boot-qemu-common.inc

COMPATIBLE_MACHINE = "^(qemu-arm)$"

U_BOOT_CONFIG = "qemu_arm_defconfig"
