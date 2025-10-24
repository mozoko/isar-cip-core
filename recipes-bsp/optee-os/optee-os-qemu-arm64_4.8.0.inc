#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2025
#
# Authors:
#  Jan Kiszka <jan.kiszka@siemens.com>
#
# SPDX-License-Identifier: MIT
#

require optee-os-qemu.inc

SRC_URI[sha256sum] = "5222cd553f5edb69ae4ec7cb99b2bfec2c47a47c0be1865b49744701918e8b4d"

OPTEE_NAME = "qemu-arm64"
OPTEE_PLATFORM = "vexpress-qemu_armv8a"
OPTEE_EXTRA_BUILDARGS += "CFG_ARM64_core=y CFG_USER_TA_TARGETS=ta_arm64"
