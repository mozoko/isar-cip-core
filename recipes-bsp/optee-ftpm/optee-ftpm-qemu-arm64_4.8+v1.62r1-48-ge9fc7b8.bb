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

require recipes-bsp/optee-ftpm/optee-ftpm.inc

SRC_URI += " \
    https://github.com/OP-TEE/optee_ftpm/archive/${SRCREV}.tar.gz;downloadfilename=optee_ftpm-${SRCREV}.tar.gz \
    https://github.com/microsoft/ms-tpm-20-ref/archive/${SRCREV_ms-tpm}.tar.gz;name=ms-tpm;downloadfilename=ms-tpm-20-ref-${SRCREV_ms-tpm}.tar.gz \
    "
SRCREV = "04cbc8a136e8fe6731a536f5fe145e2862feafd7"
SRCREV_ms-tpm = "e9fc7b89d865536c46deb63f9c7d0121a3ded49c"

SRC_URI[sha256sum] = "7adfa71c4b49affef7d4db2436d8d6b93cdc3aaa9bdc9cf795c0bf7488a4cac6"
SRC_URI[ms-tpm.sha256sum] = "b77d092c0dde362adf6bc88a580ca7c8abe124d69bb734bf28f8904ae30494a4"

S = "${WORKDIR}/optee_ftpm-${SRCREV}"
MS_TPM_20_REF_DIR = "ms-tpm-20-ref-${SRCREV_ms-tpm}"

OPTEE_NAME = "qemu-arm64"

TA_CPU = "cortex-a53"
TA_DEV_KIT_DIR = "/usr/lib/optee-os/${OPTEE_NAME}/export-ta_arm64"
