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

# StMM integration, required by UEFI auth variable management
DEPENDS:append = " edk2-standalonemm-rpmb"
DEBIAN_BUILD_DEPENDS:append = ", edk2-standalonemm-rpmb"
OPTEE_EXTRA_BUILDARGS:append = " \
    CFG_STMM_PATH=/usr/lib/edk2/BL32_AP_MM.fd \
    "

# OP-TEE fTPM integration
DEPENDS:append = " optee-ftpm-${OPTEE_NAME}"
DEBIAN_BUILD_DEPENDS:append = ", optee-ftpm-${OPTEE_NAME}"
FTPM_UUID = "bc50d971-d4c9-42c4-82cb-343fb7f37896"
OPTEE_EXTRA_BUILDARGS:append = " \
    EARLY_TA_PATHS=/usr/lib/optee-os/${OPTEE_NAME}/ta/${FTPM_UUID}.stripped.elf \
    "
