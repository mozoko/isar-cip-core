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

inherit dpkg

MAINTAINER = "cip-dev <cip-dev@lists.cip-project.org>"
DESCRIPTION = "Generate EFI certificates from single secure boot key"

PROVIDES += "secure-boot-efi-certs"
DEBIAN_PROVIDES = "secure-boot-efi-certs"

DEPENDS = "secure-boot-secrets"
DEBIAN_BUILD_DEPENDS .= ", secure-boot-secrets, efitools"

DPKG_ARCH = "all"

do_prepare_build() {
    deb_debianize

    cat <<EOF > ${S}/debian/${BPN}.install
db.auth usr/share/secure-boot-efi-certs
KEK.auth usr/share/secure-boot-efi-certs
PK.auth usr/share/secure-boot-efi-certs
EOF

    cat <<EOF >> ${S}/debian/rules

override_dh_auto_build:
	cert-to-efi-sig-list /usr/share/secure-boot-secrets/secure-boot.pem PK.esl
	sign-efi-sig-list -k /usr/share/secure-boot-secrets/secure-boot.key \
	    -c /usr/share/secure-boot-secrets/secure-boot.pem PK PK.esl PK.auth
	cp PK.auth KEK.auth
	cp PK.auth db.auth
EOF
}
