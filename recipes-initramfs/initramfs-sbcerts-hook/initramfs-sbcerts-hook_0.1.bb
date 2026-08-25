#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2026
#
# Authors:
#  Jan Kiszka <jan.kiszka@siemens.com>
#
# SPDX-License-Identifier: MIT
#

inherit initramfs-hook

MAINTAINER = "cip-dev <cip-dev@lists.cip-project.org>"
DESCRIPTION = "Initramfs hook to deploy UEFI secure boot certificates during early boot"

DEPENDS = "secure-boot-efi-certs initramfs-cip-functions"
DEBIAN_DEPENDS .= ", secure-boot-efi-certs, efitools, initramfs-cip-functions"

HOOK_COPY_EXECS = "cmp mountpoint efi-readvar efi-updatevar grep"

HOOK_ADD_MODULES = "efivarfs"

SRC_URI += " \
    file://hook \
    file://local-top \
    file://mount-stub"

do_install[cleandirs] += "${D}/usr/share/${BPN}"
do_install:append() {
    install "${WORKDIR}/mount-stub" "${D}/usr/share/${BPN}/mount"
}
