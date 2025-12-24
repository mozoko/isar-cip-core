#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2022-2025
#
# Authors:
#  Jan Kiszka <jan.kiszka@siemens.com>
#
# SPDX-License-Identifier: MIT
#

inherit u-boot

FILESPATH:append := ":${FILE_DIRNAME}/files"

SRC_URI += " \
    https://ftp.denx.de/pub/u-boot/u-boot-${PV}.tar.bz2 \
    file://0001-spl-Kconfig-Add-missing-SPL_LOAD_BLOCK-for-SYS_MMCSD.patch \
    file://0002-spl-mmc-Account-for-SYS_MMCSD_RAW_MODE_U_BOOT_USE_PA.patch \
    file://0003-spl-mmc-Respect-sector-value-passed-to-mmc_load_imag.patch \
    file://0004-arch-arm-mach-socfpga-Adjust-a-raw-sectors-for-MMC-l.patch \
    file://0005-configs-socfpga-Fix-de0_nano_soc-boot.patch \
    file://0006-configs-socfpga-Permit-EFI-booting.patch \
    file://rules.tmpl;subdir=debian"
SRC_URI[sha256sum] = "2eddf75fe261bdf2e644f4a72737e04cc9d99590c7ccac4fb0835b9e1d52f92b"

SRC_URI:append:ftpm-stmm = " \
    file://ftpm-stmm.cfg"

SRC_URI:append:secureboot = " \
    file://secure-boot.cfg"

S = "${WORKDIR}/u-boot-${PV}"

DEBIAN_BUILD_DEPENDS += ", libssl-dev:native, libssl-dev:${DISTRO_ARCH}, \
    libgnutls28-dev:native, libgnutls28-dev:${DISTRO_ARCH}"

U_BOOT_EXTRA_BUILDCMD ?= ""

TEMPLATE_VARS += "U_BOOT_EXTRA_BUILDCMD"

STATIC_KEYS_BUILD_DEPENDS = ", \
    openssl, efivar, secure-boot-secrets, python3-openssl:native"
STATIC_KEYS_BUILD_DEPENDS:append:buster   = ", pesign"
STATIC_KEYS_BUILD_DEPENDS:append:bullseye = ", pesign"
STATIC_KEYS_BUILD_DEPENDS:append:bookworm = ", pesign"
STATIC_KEYS_DEPENDS = " secure-boot-secrets"

SECURE_BOOT_BUILD_DEPENDS = "${STATIC_KEYS_BUILD_DEPENDS}"
SECURE_BOOT_BUILD_DEPENDS:ftpm-stmm = ""
SECURE_BOOT_DEPENDS = "${STATIC_KEYS_DEPENDS}"
SECURE_BOOT_DEPENDS:ftpm-stmm = ""

DEBIAN_BUILD_DEPENDS:append:secureboot = "${SECURE_BOOT_BUILD_DEPENDS}"
DEPENDS:append:secureboot = "${SECURE_BOOT_DEPENDS}"

do_prepare_build:append:ftpm-stmm() {
    sed -ni '/### Enable OPTEE, fTPM and StMM/q;p' ${S}/configs/${U_BOOT_CONFIG}
    cat ${WORKDIR}/ftpm-stmm.cfg >> ${S}/configs/${U_BOOT_CONFIG}
}

do_prepare_build:append:secureboot() {
    sed -ni '/### Secure boot config/q;p' ${S}/configs/${U_BOOT_CONFIG}
    cat ${WORKDIR}/secure-boot.cfg >> ${S}/configs/${U_BOOT_CONFIG}
}
