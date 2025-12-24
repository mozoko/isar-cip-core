#
# CIP Core, generic profile
#
# Copyright (c) Siemens, 2025
#
# Authors:
#  Jan Kiszka <jan.kiszka@siemens.com>
#
# SPDX-License-Identifier: MIT
#

inherit dpkg

SRC_URI = " \
    https://github.com/polarfire-soc/hart-software-services/archive/refs/tags/v${PV}.tar.gz;downloadfilename=hart-software-services-v${PV}.tar.gz \
    file://rules"
SRC_URI[sha256sum] = "a6e94a3d9383d4af386718ae0ba3b4be2884f5e36ccfd66b3af66166472831c4"

S = "${WORKDIR}/hart-software-services-${PV}"

DEBIAN_BUILD_DEPENDS = "libyaml-dev, libssl-dev, libelf-dev"
DEBIAN_DEPENDS = "\${shlibs:Depends}"

do_prepare_build() {
    deb_debianize
    echo "tools/hss-payload-generator/hss-payload-generator usr/bin" > ${S}/debian/${PN}.install
}
