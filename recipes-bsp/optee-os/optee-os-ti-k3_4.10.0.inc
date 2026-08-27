#
# Copyright (c) Texas Instruments Incorporated, 2025
#
# Authors:
#  Sai Sree Kartheek Adivi <s-adivi@ti.com>
#
# SPDX-License-Identifier: MIT
#

MAINTAINER = "cip-dev <cip-dev@lists.cip-project.org>"

SRC_URI += " \
    https://github.com/OP-TEE/optee_os/archive/${PV}.tar.gz;downloadfilename=optee-os-${PV}.tar.gz \
    "

SRC_URI[sha256sum] = "18633691cb075ff2249422251f1cd77c30439a95b564b46682dea1fb6580a5af"

S = "${WORKDIR}/optee_os-${PV}"

OPTEE_NAME = "ti-k3"
OPTEE_PLATFORM = "k3-am62px"
OPTEE_EXTRA_BUILDARGS += " \
	CFG_ARM64_core=y \
	CFG_USER_TA_TARGETS=ta_arm64 \
	TEE_IMPL_VERSION=${PV} \
	CFG_TEE_CORE_LOG_LEVEL=1 CFG_TEE_TA_LOG_LEVEL=1 \
	CFG_CORE_DYN_SHM=y CFG_CORE_HEAP_SIZE=524288 \
	CFG_REE_FS=n CFG_RPMB_FS=y \
	CONFIG_INSECURE=y \
	"

DEBIAN_BUILD_DEPENDS += ", device-tree-compiler, python3-cryptography:native"
