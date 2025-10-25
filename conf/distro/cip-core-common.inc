#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2019-2025
#
# Authors:
#  Jan Kiszka <jan.kiszka@siemens.com>
#
# SPDX-License-Identifier: MIT
#

KERNEL_NAME ?= "cip"

WKS_FILE ?= "${MACHINE}.wks"

CIP_IMAGE_OPTIONS ?= ""

IMAGER_BUILD_DEPS:append:bullseye = " dosfstools-native"
IMAGER_BUILD_DEPS:append:bookworm = " dosfstools-native"

PREFERRED_VERSION_linux-cip-native ?= "${PREFERRED_VERSION_linux-cip}"
PREFERRED_VERSION_linux-cip-kbuildtarget ?= "${PREFERRED_VERSION_linux-cip}"

PREFERRED_VERSION_linux-cip-rt-native ?= "${PREFERRED_VERSION_linux-cip-rt}"
PREFERRED_VERSION_linux-cip-rt-kbuildtarget ?= "${PREFERRED_VERSION_linux-cip-rt}"

TEE_SUPPLICANT_IN_USERLAND ?= "0"
