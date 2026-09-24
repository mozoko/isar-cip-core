#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2019-2026
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

PREFERRED_VERSION_linux-cip ??= "6.12.%"
PREFERRED_VERSION_linux-cip-rt ??= "6.12.%"

PREFERRED_VERSION_linux-cip-native ?= "${PREFERRED_VERSION_linux-cip}"
PREFERRED_VERSION_linux-cip-kbuildtarget ?= "${PREFERRED_VERSION_linux-cip}"

PREFERRED_VERSION_linux-cip-rt-native ?= "${PREFERRED_VERSION_linux-cip-rt}"
PREFERRED_VERSION_linux-cip-rt-kbuildtarget ?= "${PREFERRED_VERSION_linux-cip-rt}"

TEE_SUPPLICANT_IN_USERLAND ?= "0"

PREFERRED_PROVIDER_factory-reset-helper ?= "factory-reset-helper-file"
PREFERRED_PROVIDER_factory-reset-helper:ftpm-stmm = "factory-reset-helper-efi"

# Watchdog to service by initramfs hooks during lengthy operations like
# partition encryption or factory reset. In our integration, enabling
# fTPM/StMM implies a U-Boot-based firmware which may enable a watchdog early.
INITRAMFS_WATCHDOG_DEVICE ?= "${@'/dev/watchdog' \
    if int(d.getVar('WDOG_TIMEOUT') or 0) > 0 \
        or 'ftpm-stmm' in d.getVar('OVERRIDES').split(':') else ''}"
