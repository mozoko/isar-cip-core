#
# CIP Security, generic profile
#
# Copyright (c) Toshiba Corporation, 2020
#
# Authors:
#  Venkata Pyla <venkata.pyla@toshiba-tsip.com>#
#
# SPDX-License-Identifier: MIT
#

inherit dpkg-raw

MAINTAINER = "cip-dev <cip-dev@lists.cip-project.org>"

DESCRIPTION = "CIP Security configuration for IEC62443-4-2 evaluation"

SRC_URI = "file://postinst \
           file://ssh-remote-session-term.conf \
           file://ssh-pam-remote.conf \
           file://ssh-hardening.conf"

DEPENDS = "customizations sshd-regen-keys"
DEBIAN_DEPENDS = "customizations, sshd-regen-keys, libpam-google-authenticator, libpam-modules, libpam-runtime, auditd"

# Append PAM module dependencies for password strength enforcement based on the Debian version
# - libpam-cracklib: Deprecated, but still supported in Debian Buster and Bullseye.
# - libpam-passwdqc: Preferred for newer Debian releases (post-Bullseye).
DEBIAN_DEPENDS:append = ", libpam-passwdqc | libpam-cracklib"

# Append systemd PAM package to limit concurrent sessions in Debian trixie
DEBIAN_DEPENDS:append:trixie = ", libpam-systemd"

do_install[cleandirs] += "${D}/etc/ssh/sshd_config.d/"
do_install () {
    install -m 600 ${WORKDIR}/ssh-remote-session-term.conf ${D}/etc/ssh/sshd_config.d/
    install -m 600 ${WORKDIR}/ssh-pam-remote.conf ${D}/etc/ssh/sshd_config.d/
    install -m 600 ${WORKDIR}/ssh-hardening.conf ${D}/etc/ssh/sshd_config.d/
}
