#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2025
#
# Authors:
#  Jan Kiszka <jan.kiszka@siemens.com>
#
# SPDX-License-Identifier: MIT
#

require u-boot-common-${PV}.inc

COMPATIBLE_MACHINE = "de0-nano-soc"

U_BOOT_CONFIG = "socfpga_de0_nano_soc_defconfig"
U_BOOT_BIN = "u-boot-with-spl.sfp"

do_prepare_build:append() {
    sed -ni '/### DE0 Nano SoC extra config/q;p' ${S}/configs/${U_BOOT_CONFIG}
    cat <<EOF >> ${S}/configs/${U_BOOT_CONFIG}
### DE0 Nano SoC extra config
CONFIG_USE_BOOTCOMMAND=y
CONFIG_BOOTCOMMAND="bootefi bootmgr"
EOF
}
