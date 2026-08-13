#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2025-2026
#
# Authors:
#  Jan Kiszka <jan.kiszka@siemens.com>
#
# SPDX-License-Identifier: MIT
#

require optee-ftpm_${PV}.inc

OPTEE_NAME = "qemu-arm64"

TA_CPU = "cortex-a53"
TA_DEV_KIT_DIR = "/usr/lib/optee-os/${OPTEE_NAME}/export-ta_arm64"
