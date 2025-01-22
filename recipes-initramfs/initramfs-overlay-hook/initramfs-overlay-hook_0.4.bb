#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2022 - 2024
#
# Authors:
#  Jan Kiszka <jan.kiszka@siemens.com>
#  Quirin Gylstorff <quirin.gylstorff@siemens.com>
#
# SPDX-License-Identifier: MIT
#

require recipes-initramfs/initramfs-hook/hook.inc

INITRAMFS_OVERLAY_RECOVERY_SCRIPT ??= "overlay_recovery_action.script"

SRC_URI += " \
    file://local-bottom.tmpl \
    file://${INITRAMFS_OVERLAY_RECOVERY_SCRIPT} \
    "

# The variable INITRAMFS_OVERLAY_PATHS contains the directories which are
# overlayed(lower dir).
INITRAMFS_OVERLAY_PATHS ??= "/etc"

# The variable INITRAMFS_OVERLAY_STORAGE_PATH designates the path were the
# changes to the overlayed directory are stored (upper dir). The initramfs
# hook also mounts the first directory after root to
# INITRAMFS_OVERLAY_STORAGE_DEVICE if it is not mounted yet.
INITRAMFS_OVERLAY_STORAGE_PATH ??= "/var/local"

# override this to switch to UUID or PARTUUID based mounts
INITRAMFS_OVERLAY_STORAGE_DEVICE ??= "/dev/disk/by-label/var"

# options to use for mounting INITRAMFS_OVERLAY_STORAGE_DEVICE
INITRAMFS_OVERLAY_MOUNT_OPTION ??= "defaults,nodev,nosuid,noexec"

TEMPLATE_FILES += "local-bottom.tmpl"
TEMPLATE_VARS += " INITRAMFS_OVERLAY_STORAGE_PATH \
    INITRAMFS_OVERLAY_PATHS \
    INITRAMFS_OVERLAY_STORAGE_DEVICE \
    INITRAMFS_OVERLAY_MOUNT_OPTION \
    INITRAMFS_OVERLAY_RECOVERY_SCRIPT"

DEBIAN_DEPENDS .= ", awk, coreutils, util-linux, e2fsprogs"

HOOK_ADD_MODULES = "overlay"
HOOK_COPY_EXECS = "mountpoint awk e2fsck mke2fs"

SCRIPT_PREREQ = "crypt abvar"

do_install:append() {
    install -m 0755 "${WORKDIR}/${INITRAMFS_OVERLAY_RECOVERY_SCRIPT}" \
        "${D}/usr/share/initramfs-tools/scripts"
}
