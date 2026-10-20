#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2023-2026
#
# Authors:
#  Quirin Gylstorff <quirin.gylstorff@siemens.com>
#
# SPDX-License-Identifier: MIT
#

inherit image_uuid
inherit read-only-rootfs

SWUPDATE_BOOTLOADER ??= "efibootguard"
python __anonymous() {
    bootloader = d.getVar("SWUPDATE_BOOTLOADER")
    if not bootloader in ["efibootguard"]:
        bb.error(f"swupdate: SWUPDATE_BOOTLOADER set to incompatible value: '{bootloader}'")
}

include recipes-core/images/swupdate-${SWUPDATE_BOOTLOADER}.inc

IMAGE_TYPEDEP:wic += "${RO_ROOTFS_TYPE}"
IMAGE_FSTYPES:append = " swu"
# input is already compressed
SWU_COMPRESSION_TYPE = ""

# read-only rootfs with /home and /var
# as separate partitions is used
# /boot is excluded as an unified kernel image
# is used. This images are stored in separate partitions.
RO_ROOTFS_EXCLUDE_DIRS = "boot var home"

SWU_SIGNED ?= "1"
SWU_SIGN_UPDATE ??= "${SWU_SIGNED}"
SWU_VERIFY_UPDATE ??= "${SWU_SIGNED}"
SWU_HW_COMPAT ?= "cip-core-1.0"
IMAGER_BUILD_DEPS:swu += "${@'swupdate-signer' if bb.utils.to_boolean(d.getVar('SWU_SIGN_UPDATE')) else ''}"
IMAGER_INSTALL:swu += "${@'swupdate-signer' if bb.utils.to_boolean(d.getVar('SWU_SIGN_UPDATE')) else ''}"
IMAGE_INSTALL += "${@'swupdate-certificates' if bb.utils.to_boolean(d.getVar('SWU_VERIFY_UPDATE')) else ''}"

SWUPDATE_SELFBUILT ?= "0"
SWUPDATE_SELFBUILT:buster = "1"
SWUPDATE_SELFBUILT:bullseye = "1"
SWUPDATE_SELFBUILT:bookworm = "1"

python() {
    base_distro = d.getVar('BASE_DISTRO_CODENAME')
    if base_distro not in ['buster', 'bullseye', 'bookworm'] and \
       bb.utils.to_boolean(d.getVar('SWUPDATE_SELFBUILT')):
        bb.fatal(f'Self-built SWUpdate not supported for {base_distro}')

    if not bb.utils.to_boolean(d.getVar('SWUPDATE_SELFBUILT')) and \
       not bb.utils.to_boolean(d.getVar('SWU_VERIFY_UPDATE')):
        bb.fatal('SWUpdate from Debian requires signature verification for SWUs')
}

IMAGE_INSTALL:append = "${@' swupdate' if bb.utils.to_boolean(d.getVar('SWUPDATE_SELFBUILT')) else ''}"
IMAGE_PREINSTALL:append = "${@'' if bb.utils.to_boolean(d.getVar('SWUPDATE_SELFBUILT')) else ' swupdate'}"

WFX_URL ??= ""
# check for wfx url to enable support for wfx backend based updates
IMAGE_INSTALL:append = "${@' swupdate-config-wfx-' + d.getVar("MACHINE") if d.getVar("WFX_URL") else ''}"

IMAGE_INSTALL += " swupdate-handler-roundrobin"
IMAGE_INSTALL += " swupdate-config-${MACHINE}"
