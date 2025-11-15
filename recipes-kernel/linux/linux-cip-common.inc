#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2022-2025
#
# Authors:
#  Jan Kiszka <jan.kiszka@siemens.com>
#
# SPDX-License-Identifier: MIT
#

FILESEXTRAPATHS:prepend := "${FILE_DIRNAME}/files:"

require recipes-kernel/linux/linux-custom.inc
require recipes-kernel/linux/cip-kernel-config.inc

SRC_URI += "${@'file://preempt-rt.cfg' if d.getVar('KERNEL_NAME') == 'cip-rt' else ''}"
