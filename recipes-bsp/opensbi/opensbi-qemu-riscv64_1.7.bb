#
# Copyright (c) Siemens AG, 2023-2025
#
# SPDX-License-Identifier: MIT

include opensbi_${PV}.inc

inherit deploy-firmware

DEPENDS += "u-boot-qemu-riscv64"
DEBIAN_BUILD_DEPENDS .= ", u-boot-qemu-riscv64"

OPENSBI_BIN = "fw_payload.bin"
OPENSBI_EXTRA_BUILDARGS = "FW_PAYLOAD_PATH=/usr/lib/u-boot/qemu-riscv64/u-boot.bin"

do_deploy_firmware() {
    dpkg --fsys-tarfile "${DEPLOYDIR}/${PN}_${PV}_${DISTRO_ARCH}.deb" | \
        tar xOf - "./usr/lib/opensbi/${MACHINE}/${OPENSBI_BIN}" \
        > "${DEPLOYDIR_FIRMWARE}/${OPENSBI_BIN}"
}
