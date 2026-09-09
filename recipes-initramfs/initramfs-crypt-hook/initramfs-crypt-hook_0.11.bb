#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2020-2026
#
# Authors:
#  Quirin Gylstorff <quirin.gylstorff@siemens.com>
#  Jan Kiszka <jan.kiszka@siemens.com>
#
# SPDX-License-Identifier: MIT

inherit initramfs-hook

MAINTAINER = "cip-dev <cip-dev@lists.cip-project.org>"
DESCRIPTION = "Initramfs hook for TPM-backed encrypted partition setup and unlock"

RDEPENDS += "initramfs-cip-functions"

DEBIAN_DEPENDS .= ", \
    cryptsetup, \
    awk, \
    pwgen, \
    e2fsprogs, \
    tpm2-tools, \
    coreutils, \
    uuid-runtime, \
    initramfs-cip-functions"

CRYPT_BACKEND:buster = "clevis"
CRYPT_BACKEND:bullseye = "clevis"
CRYPT_BACKEND ?= "systemd"

TPM2_FUNCTIONS = "tpm2_functions"
TPM2_FUNCTIONS:buster = "tpm2_functions.buster"

OVERRIDES .= ":${CRYPT_BACKEND}"

DEBIAN_DEPENDS:append:buster = ", libgcc-7-dev, libtss2-esys0"
DEBIAN_DEPENDS:append:bullseye = ", libtss2-esys-3.0.2-0, libtss2-rc0, libtss2-mu0"
DEBIAN_DEPENDS:append:bookworm = ", libtss2-esys-3.0.2-0, libtss2-rc0, libtss2-mu0"
DEBIAN_DEPENDS:append:trixie = ", libtss2-esys-3.0.2-0t64, libtss2-rc0t64, libtss2-mu-4.0.1-0t64"
DEBIAN_DEPENDS:append:sid    = ", libtss2-esys-3.0.2-0t64, libtss2-rc0t64, libtss2-mu-4.0.1-0t64"

DEBIAN_DEPENDS:append:clevis = ", clevis-luks, jose, bash, luksmeta, file, libpwquality-tools, clevis-tpm2"
DEBIAN_DEPENDS:append:systemd:trixie = ", systemd-cryptsetup"
DEBIAN_DEPENDS:append:systemd:sid    = ", systemd-cryptsetup"
DEBIAN_DEPENDS:append:systemd = ", systemd (>= 251)"

HOOK_ADD_MODULES = " \
    tpm tpm_tis_core tpm_tis tpm_tis_spi tpm_crb dm_mod dm_crypt \
    ecb aes_generic xts"

HOOK_COPY_EXECS = " \
    pwgen mke2fs grep awk expr seq sleep basename uuidparse mountpoint \
    e2fsck resize2fs cryptsetup rm cmp \
    tpm2_pcrread tpm2_testparms tpm2_flushcontext tpm2_shutdown \
    tpm2_startauthsession tpm2_policypcr tpm2_createprimary tpm2_create \
    tpm2_load tpm2_evictcontrol tpm2_unseal tpm2_getcap \
    /usr/lib/*/libgcc_s.so.1"

HOOK_COPY_EXECS:append:clevis = " \
    clevis clevis-decrypt clevis-encrypt-tpm2 clevis-decrypt-tpm2 \
    clevis-luks-bind clevis-luks-unlock \
    clevis-luks-list clevis-luks-common-functions \
    tpm2_createpolicy \
    bash luksmeta jose sed tail sort rm mktemp pwmake file"
HOOK_COPY_EXECS:append:systemd = " \
    systemd-cryptenroll /usr/lib/systemd/systemd-cryptsetup \
    /usr/lib/*/cryptsetup/libcryptsetup-token-systemd-tpm2.so"

HOOK_COPY_EXECS:append:buster = " cryptsetup-reencrypt tpm2_pcrlist"
HOOK_COPY_EXECS:remove:buster = " \
    tpm2_pcrread tpm2_testparms tpm2_flushcontext tpm2_shutdown \
    tpm2_startauthsession tpm2_policypcr tpm2_evictcontrol tpm2_getcap \
    clevis-luks-list clevis-luks-common-functions"
HOOK_COPY_EXECS:append:bullseye = " cryptsetup-reencrypt"

SRC_URI += "file://encrypt_partition.env.tmpl \
            file://local-top-complete \
            file://encrypt_partition.${CRYPT_BACKEND}.script \
            file://${TPM2_FUNCTIONS} \
            file://local-bottom-complete \
            file://hook \
            file://pwquality.conf"

# CRYPT_PARTITIONS elements are <partition-label>:<mountpoint>:<reencrypt or format>[:expand]
CRYPT_PARTITIONS ??= "var:/var:reencrypt"
# CRYPT_CREATE_FILE_SYSTEM_CMD contains the shell command to create the filesystem
# in a newly formatted LUKS Partition
CRYPT_CREATE_FILE_SYSTEM_CMD ??= "/usr/sbin/mke2fs -t ext4"
# Timeout for creating / re-encrypting partitions on first boot
CRYPT_SETUP_TIMEOUT ??= "600"
# Watchdog to service during the initial setup of the crypto partitions
INITRAMFS_WATCHDOG_DEVICE ??= "${@'/dev/watchdog' \
    if int(d.getVar('WDOG_TIMEOUT') or 0) > 0 \
        or 'ftpm-stmm' in d.getVar('OVERRIDES').split(':') else ''}"
# clevis needs tpm hash algorithm type
CRYPT_HASH_TYPE ??= "sha256"
CRYPT_KEY_ALGORITHM ??= "ecc"
CRYPT_ENCRYPTION_OPTIONAL ??= "false"
# add support for btrfs encryption
CRYPT_BTRFS_SUPPORT ??= "0"
# mount options for encrypted partitions
INITRAMFS_CRYPT_MOUNT_OPTIONS ??= "defaults,nodev,nosuid,noexec"

TEMPLATE_VARS += "CRYPT_PARTITIONS CRYPT_CREATE_FILE_SYSTEM_CMD \
    CRYPT_SETUP_TIMEOUT INITRAMFS_WATCHDOG_DEVICE CRYPT_HASH_TYPE \
    CRYPT_KEY_ALGORITHM CRYPT_ENCRYPTION_OPTIONAL INITRAMFS_CRYPT_MOUNT_OPTIONS"
TEMPLATE_FILES += "encrypt_partition.env.tmpl"

OVERRIDES .= "${@':expand-on-crypt' if ':expand' in d.getVar('CRYPT_PARTITIONS') else ''}"
DEBIAN_DEPENDS:append:expand-on-crypt = ", fdisk, util-linux"
HOOK_COPY_EXECS:append:expand-on-crypt = " sed sfdisk tail cut dd partx rm"

OVERRIDES .= "${@':btrfs-support' if bb.utils.to_boolean(d.getVar('CRYPT_BTRFS_SUPPORT')) else ''}"
DEBIAN_DEPENDS:append:btrfs-support = ", btrfs-progs"
HOOK_COPY_EXECS:append:btrfs-support = " mkdir rmdir btrfs"

do_install[cleandirs] += "${D}/usr/share/encrypt_partition"
do_install:prepend() {
    install -m 0600 "${WORKDIR}/encrypt_partition.env" "${D}/usr/share/encrypt_partition/encrypt_partition.env"
    install -m 0644 "${WORKDIR}/pwquality.conf" "${D}/usr/share/encrypt_partition/pwquality.conf"
    install -m 0755 "${WORKDIR}/encrypt_partition.${CRYPT_BACKEND}.script" \
        "${D}/usr/share/encrypt_partition/encrypt_partition_tpm2"
    install -m 0755 "${WORKDIR}/${TPM2_FUNCTIONS}" \
        "${D}/usr/share/encrypt_partition/tpm2_functions"
}
