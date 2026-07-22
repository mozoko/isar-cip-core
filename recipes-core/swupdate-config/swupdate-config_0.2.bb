#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2024
#
# Authors:
#  Quirin Gylstorff <quirin.gylstorff@siemens.com>
#
# SPDX-License-Identifier: MIT
#

inherit dpkg-raw

MAINTAINER = "cip-dev <cip-dev@lists.cip-project.org>"
DESCRIPTION = "SWUpdate base configuration"

PROVIDES := "${PN}"
DEBIAN_PROVIDES := "${PN}"

PN .= "-${MACHINE}"

SRC_URI = "file://swupdate.cfg.tmpl \
           file://hwrevision.tmpl"

SWUPDATE_DIGEST_PROVIDER = "digest-provider = "pkcs#7mbedtls";"
SWUPDATE_DIGEST_PROVIDER:trixie = ""
SWUPDATE_DIGEST_PROVIDER:bookworm = ""
SWUPDATE_DIGEST_PROVIDER:bullseye = ""
SWUPDATE_DIGEST_PROVIDER:buster = ""

MACHINE_HW_VERSION ??= "cip-core-1.0"
TEMPLATE_FILES += "hwrevision.tmpl swupdate.cfg.tmpl"
TEMPLATE_VARS += "MACHINE MACHINE_HW_VERSION SWUPDATE_DIGEST_PROVIDER"

do_install[cleandirs] = "${D}/etc/"
do_install() {
    install -v -m 644 "${WORKDIR}"/swupdate.cfg "${D}"/etc/
    if [ -n "${MACHINE_HW_VERSION}" ]; then
        install -v -m 644 "${WORKDIR}"/hwrevision "${D}"/etc/
    fi
}
