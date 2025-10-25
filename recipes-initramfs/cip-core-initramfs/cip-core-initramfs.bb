#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2021 - 2025
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
