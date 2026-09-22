#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2022-2026
#
# Authors:
#  Jan Kiszka <jan.kiszka@siemens.com>
#
# SPDX-License-Identifier: MIT
#

FILESEXTRAPATHS:prepend := "${FILE_DIRNAME}/files:"

inherit linux-kernel

MAINTAINER = "cip-dev <cip-dev@lists.cip-project.org>"

require recipes-kernel/linux/cip-kernel-config.inc
require recipes-kernel/linux/kernel-hardening.inc

KERNEL_CONFIG_FRAGMENTS:append = "${@(' ' + d.getVar('RT_FRAGMENT')) if d.getVar('KERNEL_NAME') == 'cip-rt' else ''}"

python() {
    if d.getVar('RT_FRAGMENT') == '' and d.getVar('KERNEL_NAME') == 'cip-rt':
        bb.fatal("No RT_FRAGMENT provided for cip-rt kernel")
}
