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

SRC_URI[sha256sum] = "18633691cb075ff2249422251f1cd77c30439a95b564b46682dea1fb6580a5af"

OPTEE_NAME = "qemu-arm64"
OPTEE_PLATFORM = "vexpress-qemu_armv8a"
OPTEE_EXTRA_BUILDARGS += "CFG_ARM64_core=y CFG_USER_TA_TARGETS=ta_arm64"
