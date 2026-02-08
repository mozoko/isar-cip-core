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

PROVIDES := "${PN}"
DEBIAN_PROVIDES := "${PN}"

PN .= "-${MACHINE}"

SRC_URI = "file://swupdate.cfg.tmpl \
           file://hwrevision.tmpl"

MACHINE_HW_VERSION ??= "cip-core-1.0"
WFX_ADDITIONAL_HTTP_HEADERS_SECTION ??= ""
TEMPLATE_FILES += "hwrevision.tmpl swupdate.cfg.tmpl"
TEMPLATE_VARS += "MACHINE MACHINE_HW_VERSION WFX_ADDITIONAL_HTTP_HEADERS_SECTION"

python do_add_http_header_section() {
    if "wfx-onboard" in d.getVar('OVERRIDES'):
        additional_http_headers = f"""
custom-http-headers: (
    {{ name = "X-Machine-Serial-No"; value="#MACHINE-ID#" }},
);
        """
        d.setVar('WFX_ADDITIONAL_HTTP_HEADERS_SECTION', additional_http_headers)
}
do_transform_template[prefuncs] += "do_add_http_header_section"

do_install[cleandirs] = "${D}/etc/"
do_install() {
    install -v -m 644 "${WORKDIR}"/swupdate.cfg "${D}"/etc/
    if [ -n "${MACHINE_HW_VERSION}" ]; then
        install -v -m 644 "${WORKDIR}"/hwrevision "${D}"/etc/
    fi
}
