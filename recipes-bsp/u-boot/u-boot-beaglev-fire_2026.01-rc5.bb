#
# CIP Core, generic profile
#
# Copyright (c) Siemens, 2025
#
# Authors:
#  Jan Kiszka <jan.kiszka@siemens.com>
#
# SPDX-License-Identifier: MIT
#

inherit u-boot

COMPATIBLE_MACHINE = "^(beaglev-fire)$"

FILESPATH:append := ":${FILE_DIRNAME}/files"

MCHP_RELEASE = "${@d.getVar('PV').split('-', 1)[1]}"

SRC_URI += " \
    https://ftp.denx.de/pub/u-boot/u-boot-${PV}.tar.bz2 \
    file://0001-riscv-create-a-custom-CPU-implementation-for-PolarFi.patch \
    file://0002-riscv-mpfs-move-SoC-level-options-to-the-CPU-Kconfig.patch \
    file://0003-riscv-Add-support-for-BeagleV-Fire.patch \
    file://rules.tmpl;subdir=debian \
    file://config.yaml;subdir=debian"
SRC_URI[sha256sum] = "2eddf75fe261bdf2e644f4a72737e04cc9d99590c7ccac4fb0835b9e1d52f92b"

S = "${WORKDIR}/u-boot-${PV}"

DEPENDS += "hss-payload-generator-native"
DEBIAN_BUILD_DEPENDS += ", libssl-dev:native, libssl-dev, \
    libgnutls28-dev:native, libgnutls28-dev, \
    hss-payload-generator:native"

U_BOOT_CONFIG = "beaglev_fire_defconfig"
U_BOOT_BIN = "u-boot.bin"
U_BOOT_EXTRA_BUILDCMD = "hss-payload-generator -c debian/config.yaml payload.bin"

TEMPLATE_VARS += "U_BOOT_EXTRA_BUILDCMD"

do_prepare_build:append() {
    echo "payload.bin /usr/lib/u-boot/${MACHINE}" > \
        ${S}/debian/u-boot-${MACHINE}.install
}
