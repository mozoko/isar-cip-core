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

inherit optee-ftpm

MAINTAINER = "cip-dev <cip-dev@lists.cip-project.org>"

SRC_URI += " \
    https://github.com/OP-TEE/optee_ftpm/archive/${SRCREV}.tar.gz;downloadfilename=optee_ftpm-${SRCREV}.tar.gz \
    file://0001-Update-to-TPM-2.0-Reference-Implementation-v184.patch \
    file://0002-Disable-command-TPM2_CertifyX509.patch \
    https://github.com/TrustedComputingGroup/TPM/archive/${SRCREV_tpm}.tar.gz;name=tpm;downloadfilename=tpm-${SRCREV_tpm}.tar.gz \
    file://0001-Address-improper-object-slot-reuse.patch;patchdir=${WORKDIR}/${MS_TPM_20_REF_DIR} \
    file://0002-Address-RSA-OAEP-timing-side-channel-vulnerability.patch;patchdir=${WORKDIR}/${MS_TPM_20_REF_DIR} \
    "
SRCREV = "a09269b15de635e1816fe832e26adfbfb44c5455"
SRCREV_tpm = "bc29a21d44b01396223c152a4834e52318591770"

SRC_URI[sha256sum] = "1ad448048fa8e507459fb24cc1e7e1a42099303a272c8804f8d62894090d047b"
SRC_URI[tpm.sha256sum] = "e634a4a3834d069e327da44d151d77915d3bd6e2ee8763c35f291b450a863083"

S = "${WORKDIR}/optee_ftpm-${SRCREV}"
MS_TPM_20_REF_DIR = "TPM-${SRCREV_tpm}"
