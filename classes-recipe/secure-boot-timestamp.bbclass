#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2026
#
# Authors:
#  Felix Moessbauer <felix.moessbauer@siemens.com>
#
# SPDX-License-Identifier: MIT
#
# Provide up-to-date timestamps for the cryptographic operations
# of the secure boot tooling and reflect that in the Debian changelog.
# When calling the SB tooling, ensure to use set TZ=UTC as well.

DEBIAN_CHANGELOG_TIMESTAMP ?= "${SOURCE_DATE_EPOCH}"
SECURE_BOOT_TIMESTAMP = "${@time.strftime('%Y-%m-%d %H:%M:%S', time.gmtime(int(d.getVar('DEBIAN_CHANGELOG_TIMESTAMP'))))}"

issue_timestamp_note() {
    bbnote "Use timestamp ${SECURE_BOOT_TIMESTAMP} UTC for signing"
}
do_prepare_build[prefuncs] += "issue_timestamp_note"
