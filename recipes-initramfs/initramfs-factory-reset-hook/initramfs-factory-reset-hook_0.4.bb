#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2025 - 2026
#
# Authors:
#  Quirin Gylstorff <quirin.gylstorff@siemens.com>
#
# SPDX-License-Identifier: MIT

inherit initramfs-hook

MAINTAINER = "cip-dev <cip-dev@lists.cip-project.org>"

DESCRIPTION = "Delete the content of the given Devices"

# list of partitions by label
INITRAMFS_FACTORY_RESET_DEVICES ??= "/dev/disk/by-partlabel/var"
INITRAMFS_FACTORY_RESET_LUKS_FORMAT_TYPE ??= "ext4"
INITRAMFS_FACTORY_RESET_CLEAR_TPM ??= '0'
INITRAMFS_FACTORY_RESET_CLEAR_TPM:encrypt-partitions ?= '1'
INITRAMFS_FACTORY_RESET_CLEAR_TPM_BOOL = "${@ '1' if bb.utils.to_boolean(d.getVar('INITRAMFS_FACTORY_RESET_CLEAR_TPM')) else '0'}"
# Timeout in seconds for wiping the partitions during factory reset,
# defaults to INITRAMFS_SETUP_TIMEOUT or 600 if that is unset
INITRAMFS_FACTORY_RESET_TIMEOUT ?= "${@ d.getVar('INITRAMFS_SETUP_TIMEOUT') or 600}"
# Watchdog to service during the factory reset,
# the default is set in the distro configuration
INITRAMFS_WATCHDOG_DEVICE ??= ""

# to support factory reset on btrfs, add 'btrfs'
INITRAMFS_FACTORY_RESET_FSTYPES = "${INITRAMFS_FACTORY_RESET_LUKS_FORMAT_TYPE}"

SRC_URI += " \
    file://factory-reset-script.tmpl \
    file://reset-env.tmpl \
    file://hook"

FACTORY_RESET_HOOK_TARGET = "${@ 'local-top' if bb.utils.to_boolean(d.getVar('INITRAMFS_FACTORY_RESET_CLEAR_TPM')) else 'local-bottom'}"

TEMPLATE_FILES += "reset-env.tmpl factory-reset-script.tmpl"
TEMPLATE_VARS += " INITRAMFS_FACTORY_RESET_DEVICES \
                   INITRAMFS_FACTORY_RESET_LUKS_FORMAT_TYPE \
                   INITRAMFS_FACTORY_RESET_CLEAR_TPM_BOOL \
                   INITRAMFS_WATCHDOG_DEVICE \
                   INITRAMFS_FACTORY_RESET_TIMEOUT"

RDEPENDS = "factory-reset-helper \
            initramfs-cip-functions"

DEBIAN_DEPENDS .= ", coreutils, util-linux, e2fsprogs, awk, \
                    factory-reset-helper, findutils, initramfs-cip-functions"
DEBIAN_DEPENDS:append:encrypt-partitions = ", tpm2-tools"
HOOK_COPY_EXECS = "mountpoint findmnt mktemp basename dd \
                   mke2fs awk blkid rm get-factory-reset.sh \
                   chattr grep find blockdev seq"
HOOK_COPY_EXECS:append:encrypt-partitions = " tpm2_clear"

OVERRIDES .= "${@':btrfs-support' if 'btrfs' in d.getVar('INITRAMFS_FACTORY_RESET_FSTYPES') else ''}"
DEBIAN_DEPENDS:append:btrfs-support = ", btrfs-progs"
HOOK_COPY_EXECS:append:btrfs-support = " mkdir rmdir mkfs.btrfs"

HOOK_ADD_MODULES = "efivarfs"

do_install[cleandirs] += "${D}/usr/share/factory-reset/"
do_install:prepend() {
    rm -f ${WORKDIR}/local-top ${WORKDIR}/local-bottom
    ln -sf ${WORKDIR}/factory-reset-script ${WORKDIR}/${FACTORY_RESET_HOOK_TARGET}
    install -m 0755 "${WORKDIR}/reset-env" \
        "${D}/usr/share/factory-reset/reset-env"
}
