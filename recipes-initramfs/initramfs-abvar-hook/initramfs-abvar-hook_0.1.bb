#
# CIP Core, generic profile
#
# Copyright (c) Siemens, 2025
#
# Authors:
#  Jan Kiszka <jan.kiszka@siemens.com>
#
# SPDX-License-Identifier: MIT

require recipes-initramfs/initramfs-hook/hook.inc

SRC_URI += " \
    file://local-bottom.tmpl"

# Override this to switch to filesystem UUID based mounts
# Note: Must use labels or fs UUID with encrypting the partition!
INITRAMFS_VAR_DEVICE ??= "/dev/disk/by-label/var"

INITRAMFS_VAR_MOUNT_OPTIONS ??= "defaults,nodev,nosuid,noexec"

TEMPLATE_FILES += "local-bottom.tmpl"
TEMPLATE_VARS += "\
    INITRAMFS_VAR_DEVICE \
    INITRAMFS_VAR_MOUNT_OPTIONS"

HOOK_ADD_MODULES = "btrfs"
HOOK_COPY_EXECS = "btrfs grep rmdir bg_printenv"

DEBIAN_DEPENDS .= ", btrfs-progs, efibootguard-tools|efibootguard"
