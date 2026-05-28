#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2025-2026
#
# Authors:
#  Jan Kiszka <jan.kiszka@siemens.com>
#  Benedikt Niedermayr <benedikt.niedermayr@siemens.com>
#
# SPDX-License-Identifier: MIT
#

inherit optee-ftpm

MAINTAINER = "cip-dev <cip-dev@lists.cip-project.org>"

# Note: Referring to the forks which are currently upstreamed
SRC_URI += "\
    git://github.com/svenbauer-work/TPM.git;protocol=https;branch=fix/port-fixes-by-jan-kiszka;name=TPM;destsuffix=TPM \
    git://github.com/svenbauer-work/optee_ftpm.git;protocol=https;branch=feat/support-tpm-v184;name=optee_ftpm;destsuffix=optee_ftpm \
    "

SRCREV_TPM = "a9665960ddfb92b8a140cd8d37d29342ddb05b3b"
SRCREV_optee_ftpm = "5f6f753d9f1428811420734d5f312249d0aa7ece"

S = "${WORKDIR}/optee_ftpm"
MS_TPM_20_REF_DIR = "TPM"

OPTEE_NAME = "qemu-arm64"

TA_CPU = "cortex-a53"
TA_DEV_KIT_DIR = "/usr/lib/optee-os/${OPTEE_NAME}/export-ta_arm64"
