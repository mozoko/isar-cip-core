#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2021 - 2026
#
# Authors:
#  Quirin Gylstorff <quirin.gylstorff@siemens.com>
#
# SPDX-License-Identifier: MIT
#

inherit initramfs

INITRAMFS_INSTALL += " \
    initramfs-overlay-hook \
    "

INITRAMFS_INSTALL:append:encrypt-partitions = " initramfs-crypt-hook"
INITRAMFS_INSTALL:append:factory-reset = " initramfs-factory-reset-hook"
INITRAMFS_INSTALL:append:ftpm-stmm = " initramfs-tee-ftpm-hook"
INITRAMFS_INSTALL:append:swupdate = " initramfs-abrootfs-hook"
INITRAMFS_INSTALL:append:swupdate = " initramfs-${RO_ROOTFS_TYPE}-hook"
INITRAMFS_INSTALL:append:secureboot = " initramfs-verity-hook"
# abrootfs cannot be installed together with verity
INITRAMFS_INSTALL:remove:secureboot = "initramfs-abrootfs-hook"
