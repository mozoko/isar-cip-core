#
# Copyright (c) Texas Instruments Incorporated, 2025
#
# Authors:
#  Sai Sree Kartheek Adivi <s-adivi@ti.com>
#
# SPDX-License-Identifier: MIT
#

inherit optee-os

require optee-os-ti-k3_${PV}.inc

# StMM integration, required by UEFI auth variable management
DEPENDS:append:optee-with-apps = " edk2-standalonemm-rpmb"
DEBIAN_BUILD_DEPENDS:append:optee-with-apps = ", edk2-standalonemm-rpmb"
OPTEE_EXTRA_BUILDARGS:append:optee-with-apps = " \
	CFG_STMM_PATH=/usr/lib/edk2/BL32_AP_MM.fd \
	"

# OP-TEE fTPM integration
DEPENDS:append:optee-with-apps = " optee-ftpm-${OPTEE_NAME}"
DEBIAN_BUILD_DEPENDS:append:optee-with-apps = ", optee-ftpm-${OPTEE_NAME}"
FTPM_UUID = "bc50d971-d4c9-42c4-82cb-343fb7f37896"
OPTEE_EXTRA_BUILDARGS:append:optee-with-apps = " \
	CFG_EARLY_TA=y \
	EARLY_TA_PATHS=/usr/lib/optee-os/${OPTEE_NAME}/ta/${FTPM_UUID}.stripped.elf \
	"
