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

require recipes-bsp/u-boot/u-boot-custom.inc

FILESPATH:append := ":${FILE_DIRNAME}/files"

SRC_URI += " \
    https://ftp.denx.de/pub/u-boot/u-boot-${PV}.tar.bz2 \
    file://0001-tools-Do-not-generate-logo-when-cross-building.patch \
    file://rules.tmpl;subdir=debian"
SRC_URI[sha256sum] = "0f933f6c5a426895bf306e93e6ac53c60870e4b54cda56d95211bec99e63bec7"

SRC_URI:append:secureboot = " \
    file://secure-boot.cfg"

S = "${WORKDIR}/u-boot-${PV}"

DEBIAN_BUILD_DEPENDS += ", libssl-dev:native, libssl-dev:${DISTRO_ARCH}, \
    libgnutls28-dev:native, libgnutls28-dev:${DISTRO_ARCH}"

DEBIAN_BUILD_DEPENDS:append:secureboot = ", \
    openssl, efivar, secure-boot-secrets, python3-openssl:native"
DEBIAN_BUILD_DEPENDS:append:secureboot:buster   = ", pesign"
DEBIAN_BUILD_DEPENDS:append:secureboot:bullseye = ", pesign"
DEBIAN_BUILD_DEPENDS:append:secureboot:bookworm = ", pesign"
DEPENDS:append:secureboot = " secure-boot-secrets"

do_prepare_build:append:secureboot() {
    sed -ni '/### Secure boot config/q;p' ${S}/configs/${U_BOOT_CONFIG}
    cat ${WORKDIR}/secure-boot.cfg >> ${S}/configs/${U_BOOT_CONFIG}
}
