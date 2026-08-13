#
# Copyright (c) Texas Instruments Incorporated, 2025
#
# Authors:
#  Sai Sree Kartheek Adivi <s-adivi@ti.com>
#
# SPDX-License-Identifier: MIT
#

require recipes-bsp/optee-ftpm/optee-ftpm_${PV}.inc

OPTEE_NAME = "ti-k3"

TA_CPU = "cortex-a53"
TA_DEV_KIT_DIR = "/usr/lib/optee-os/${OPTEE_NAME}/export-ta_arm64"
