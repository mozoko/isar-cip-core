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

SRC_URI += " \
    https://github.com/OP-TEE/optee_os/archive/${PV}.tar.gz;downloadfilename=optee-os-${PV}.tar.gz \
    "

S = "${WORKDIR}/optee_os-${PV}"

DEBIAN_BUILD_DEPENDS += ", device-tree-compiler, python3-cryptography:native"

OPTEE_EXTRA_BUILDARGS = " \
    TEE_IMPL_VERSION=${PV} \
    CFG_TEE_CORE_LOG_LEVEL=2 CFG_TEE_TA_LOG_LEVEL=2 \
    CFG_REE_FS=n CFG_CORE_HEAP_SIZE=524288 CFG_RPMB_FS=y \
    CFG_RPMB_WRITE_KEY=y CFG_RPMB_TESTKEY=y \
    "

OPTEE_ENABLE_APPS ?= "1"
OVERRIDES .= "${@':optee-with-apps' if bb.utils.to_boolean(d.getVar("OPTEE_ENABLE_APPS")) else ''}"

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
