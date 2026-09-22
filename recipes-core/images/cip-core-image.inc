#
# CIP Core
#
# Copyright (c) Siemens AG, 2019-2024
#
# Authors:
#  Jan Kiszka <jan.kiszka@siemens.com>
#
# SPDX-License-Identifier: MIT
#

inherit image
inherit_defer ${@'cis-hardening' if bb.utils.to_boolean(d.getVar('CIP_CIS_HARDENING')) else ''}

IMAGE_INSTALL:append:factory-reset = "factory-reset-helper"
IMAGE_INSTALL:append = "${@' cip-cis-rules-config-${MACHINE}' if bb.utils.to_boolean(d.getVar('CIP_CIS_HARDENING')) else ''}"
IMAGE_INSTALL:append = "${@' cis-validation' if bb.utils.to_boolean(d.getVar('CIP_CIS_HARDENING_VALIDATION')) else ''}"

ISAR_RELEASE_CMD = "git -C ${LAYERDIR_cip-core} describe --tags --dirty --always --match 'v[0-9].[0-9]*'"
