#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2026
#
# SPDX-License-Identifier: MIT

OVERRIDES:append = "${@':kernel-hardening' if bb.utils.to_boolean(d.getVar('CIP_CIS_HARDENING')) else ''}"

KERNEL_CONFIG_FRAGMENTS:append:kernel-hardening = " hardening.kernel.cfg"

do_dpkg_source[depends] += "${@'cip-cis-rules-config:do_deploy_hardening' if bb.utils.to_boolean(d.getVar('CIP_CIS_HARDENING')) else ''}"

do_dpkg_source:prepend:kernel-hardening() {
    cp ${DEPLOY_DIR_IMAGE}/${CIP_CIS_HARDENING_KCONFIG} ${WORKDIR}/hardening.kernel.cfg
}
