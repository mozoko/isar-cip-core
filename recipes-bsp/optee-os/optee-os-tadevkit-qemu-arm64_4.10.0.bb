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

inherit optee-os-tadevkit

OPTEE_ENABLE_APPS = "0"

require optee-os-qemu-arm64_${PV}.inc
