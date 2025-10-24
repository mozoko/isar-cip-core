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

inherit deploy-firmware

require recipes-bsp/trusted-firmware-a/trusted-firmware-a-custom.inc

SRC_URI += "https://github.com/TrustedFirmware-A/trusted-firmware-a/archive/refs/tags/v${PV}.tar.gz;downloadfilename=trusted-firmware-a-v${PV}.tar.gz"
SRC_URI[sha256sum] = "28bc15daeeed000ecd30819ecc4851bf9ffc2d33e1d4553a71985c17f47a999e"

S = "${WORKDIR}/trusted-firmware-a-${PV}"

DEPENDS += "u-boot-qemu-arm64 optee-os-qemu-arm64"
DEBIAN_BUILD_DEPENDS += ", libssl-dev:native, u-boot-qemu-arm64, optee-os-qemu-arm64"

TF_A_NAME = "qemu-arm64"
TF_A_PLATFORM = "qemu"
TF_A_EXTRA_BUILDARGS = " \
    SPD=opteed BL32_RAM_LOCATION=tdram \
    BL32=/usr/lib/optee-os/qemu-arm64/tee-raw.bin \
    BL33=/usr/lib/u-boot/qemu-arm64/u-boot.bin \
    all fip"
TF_A_BINARIES = "release/bl1.bin release/fip.bin"

do_deploy_firmware() {
    dpkg --fsys-tarfile "${WORKDIR}/trusted-firmware-a-${TF_A_NAME}_${PV}_${DISTRO_ARCH}.deb" | \
        tar xOf - "./usr/lib/trusted-firmware-a/${TF_A_NAME}/bl1.bin" \
        > "${DEPLOYDIR_FIRMWARE}/firmware.bin"
    dpkg --fsys-tarfile "${WORKDIR}/trusted-firmware-a-${TF_A_NAME}_${PV}_${DISTRO_ARCH}.deb" | \
        tar xOf - "./usr/lib/trusted-firmware-a/${TF_A_NAME}/fip.bin" | \
        dd of="${DEPLOYDIR_FIRMWARE}/firmware.bin" seek=64 bs=4096 conv=notrunc
}
