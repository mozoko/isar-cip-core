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

inherit deploy-firmware

SRC_URI += "file://qemu-extra.cfg"

U_BOOT_BIN = "u-boot.bin"

do_prepare_build:append() {
    sed -ni '/### QEMU extra config/q;p' ${S}/configs/${U_BOOT_CONFIG}
    cat ${WORKDIR}/qemu-extra.cfg >> ${S}/configs/${U_BOOT_CONFIG}
}

do_deploy_firmware() {
    dpkg --fsys-tarfile "${WORKDIR}/u-boot-${MACHINE}_${PV}_${DISTRO_ARCH}.deb" | \
        tar xOf - "./usr/lib/u-boot/${MACHINE}/${U_BOOT_BIN}" \
        > "${DEPLOYDIR_FIRMWARE}/firmware.bin"
}
