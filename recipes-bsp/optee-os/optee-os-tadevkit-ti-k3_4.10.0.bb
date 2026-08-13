#
# Copyright (c) Texas Instruments Incorporated, 2025
#
# Authors:
#  Sai Sree Kartheek Adivi <s-adivi@ti.com>
#
# SPDX-License-Identifier: MIT
#

inherit optee-os-tadevkit

OPTEE_ENABLE_APPS = "0"

require optee-os-ti-k3_${PV}.inc
