#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2019-2025
#
# Authors:
#  Jan Kiszka <jan.kiszka@siemens.com>
#
# SPDX-License-Identifier: MIT
#

require common.inc

SRC_URI += "file://ssh-permit-root.conf"
SRC_URI:remove:security = "file://ssh-permit-root.conf"

DEBIAN_DEPENDS:append:secureboot = ", efitools"

DESCRIPTION = "CIP Core image demo & customizations"

do_prepare_build:prepend:qemu-riscv64() {
	if ! grep -q serial-getty@hvc0.service ${WORKDIR}/postinst; then
		# suppress SBI console - overlaps with serial console
		echo >> ${WORKDIR}/postinst
		echo "systemctl mask serial-getty@hvc0.service" >> ${WORKDIR}/postinst
	fi
}

do_install[cleandirs] += "${D}/etc/ssh/sshd_config.d/"
do_install:append () {
	if [ -f "${WORKDIR}/ssh-permit-root.conf" ]; then
		install -v -m 644 ${WORKDIR}/ssh-permit-root.conf ${D}/etc/ssh/sshd_config.d/
	fi
}
