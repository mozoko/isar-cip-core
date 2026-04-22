# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2026
#
# Authors:
#  Quirin Gylstorff <quirin.gylstorff@siemens.com>
#
# SPDX-License-Identifier: MIT
#
DESCRIPTION = "CIS validation for CIP based images"
MAINTAINER = "Clara Kowalsky <clara.kowalsky@siemens.com>"

inherit dpkg-raw

SRC_URI = "file://${BPN}.tmpl \
           file://${BPN}.service \
           file://generate-overrides.py \
           file://postinst \
           "

RDEPENDS:${PN} += "cis-hardening cip-cis-rules-config-${MACHINE}"
DEBIAN_DEPENDS += "cis-hardening, cip-cis-rules-config-${MACHINE},"

CIS ?= "/opt/cis-hardening"
TEMPLATE_FILES += "${BPN}.tmpl"
TEMPLATE_VARS += "CIS"

do_generate_overrides[depends] += "cip-cis-rules-config:do_deploy_hardening"
do_generate_overrides() {
    if ! "${WORKDIR}/generate-overrides.py" \
        --config-json "${DEPLOY_DIR_IMAGE}/${CIP_CIS_HARDENING_CONFIG}" \
        --output "${WORKDIR}/overrides"; then
        bbfatal "Failed to generate override files."
    fi
}
addtask generate_overrides before do_install after do_unpack

OVERRIDES_DIR ?= "/usr/share/${PN}/overrides"
TEMPLATE_VARS += "OVERRIDES_DIR"

do_install[cleandirs] += "${D}/usr/bin \
                          ${D}${OVERRIDES_DIR} \
                         "

do_install() {
    install -m 755 ${WORKDIR}/${BPN} ${D}/usr/bin

    for cfg in "${WORKDIR}"/overrides/*.cfg; do
        [ -f "$cfg" ] || continue
        install -m 644 "$cfg" "${D}${OVERRIDES_DIR}"
    done
}
