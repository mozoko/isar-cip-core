#
# Copyright (c) Texas Instruments Incorporated, 2025
#
# Authors:
#  Sai Sree Kartheek Adivi <s-adivi@ti.com>
#
# SPDX-License-Identifier: MIT
#

require u-boot-common-${PV}.inc

TI_FIRMWARE_PV = "11.01.16"

SRC_URI += " \
	https://github.com/TexasInstruments/ti-linux-firmware/raw/refs/tags/${TI_FIRMWARE_PV}/ti-sysfw/ti-fs-firmware-am62px-hs-fs-cert.bin;downloadfilename=ti-fs-firmware-am62px-hs-fs-cert.bin;name=am62p-sysfw-cert-hs-fs \
	https://github.com/TexasInstruments/ti-linux-firmware/raw/refs/tags/${TI_FIRMWARE_PV}/ti-sysfw/ti-fs-firmware-am62px-hs-fs-enc.bin;downloadfilename=ti-fs-firmware-am62px-hs-fs-enc.bin;name=am62p-sysfw-enc-hs-fs \
	https://github.com/TexasInstruments/ti-linux-firmware/raw/refs/tags/${TI_FIRMWARE_PV}/ti-sysfw/ti-fs-stub-firmware-am62px-hs-cert.bin;downloadfilename=ti-fs-stub-firmware-am62px-hs-cert.bin;name=am62p-sysfw-stub-cert \
	https://github.com/TexasInstruments/ti-linux-firmware/raw/refs/tags/${TI_FIRMWARE_PV}/ti-sysfw/ti-fs-stub-firmware-am62px-hs-enc.bin;downloadfilename=ti-fs-stub-firmware-am62px-hs-enc.bin;name=am62p-sysfw-stub-enc \
	https://github.com/TexasInstruments/ti-linux-firmware/raw/refs/tags/${TI_FIRMWARE_PV}/ti-sysfw/ti-fs-firmware-am62px-hs-cert.bin;downloadfilename=ti-fs-firmware-am62px-hs-cert.bin;name=am62p-sysfw-cert-hs \
	https://github.com/TexasInstruments/ti-linux-firmware/raw/refs/tags/${TI_FIRMWARE_PV}/ti-sysfw/ti-fs-firmware-am62px-hs-enc.bin;downloadfilename=ti-fs-firmware-am62px-hs-enc.bin;name=am62p-sysfw-enc-hs \
	https://github.com/TexasInstruments/ti-linux-firmware/raw/refs/tags/${TI_FIRMWARE_PV}/ti-dm/am62pxx/ipc_echo_testb_mcu1_0_release_strip.xer5f;downloadfilename=ipc_echo_testb_mcu1_0_release_strip.xer5f;name=am62p-dm \
	file://rules-${MACHINE} \
	file://ti-extra.cfg \
	file://0001-configs-KASLR-OPTEE-RNG-support-for-K3-devic.patch \
	"

SRC_URI[am62p-dm.sha256sum] = "85ba372a3c9cd0d09c84f9bbe46a72c7e4d6fdaff67df3ee7fbec3996965f044"
SRC_URI[am62p-sysfw-cert-hs-fs.sha256sum] = "7781f2e7cc6741077f502692a93f435e7aa185de14d6aa8bb33d4f32f998f47a"
SRC_URI[am62p-sysfw-enc-hs-fs.sha256sum] = "4563a2b22568581e04eeacc468feef5e204020c0d09a53351fd62d109e72b88f"
SRC_URI[am62p-sysfw-stub-cert.sha256sum] = "dc45dcbc3dec319a9f3400a7051b692fe3c5e2369b33a46e8ef8c81419557eaa"
SRC_URI[am62p-sysfw-stub-enc.sha256sum] = "9d6537148e7071f7ffbe33231c2e8ba1f17db398dff96c0a423baa8afd9e0bc8"
SRC_URI[am62p-sysfw-cert-hs.sha256sum] = "ec3a7783d7dc6aab1085dfd131cbd08ad5f895b7f22530c90a93333c4cd22603"
SRC_URI[am62p-sysfw-enc-hs.sha256sum] = "4d0ed21a36dd57ada78307506403204e179d81dd1f05b3db384d4b24e17c5b9d"

S = "${WORKDIR}/u-boot-${PV}"
TI_LINUX_FIRMWARE = "${S}/ti-linux-firmware"

COMPATIBLE_MACHINE = "ti-am62px-sk"

U_BOOT_R5_CONFIG = "am62px_evm_r5_defconfig"
U_BOOT_A53_CONFIG = "am62px_evm_a53_defconfig"
U_BOOT_CONFIG = "${U_BOOT_A53_CONFIG}"

U_BOOT_BIN_INSTALL = "tiboot3-am62px-hs-fs-evm.bin tispl.bin u-boot.img"

OVERRIDES .= ":ftpm-stmm"

DEPENDS += "trusted-firmware-a-ti-k3 optee-os-ti-k3"
DEBIAN_BUILD_DEPENDS =. "gcc-arm-linux-gnueabihf, \
    swig, python3-dev:native, python3-setuptools, python3-pyelftools, \
    python3-jsonschema:native, yamllint:native, \
    trusted-firmware-a-ti-k3, optee-os-ti-k3,"

do_prepare_build:append() {
    mkdir -p ${TI_LINUX_FIRMWARE}/ti-sysfw
    cp ${WORKDIR}/ti-fs-*firmware-am62px-hs*.bin ${TI_LINUX_FIRMWARE}/ti-sysfw
    mkdir -p ${TI_LINUX_FIRMWARE}/ti-dm/am62pxx
    cp ${WORKDIR}/ipc_echo_testb_mcu1_0_release_strip.xer5f ${TI_LINUX_FIRMWARE}/ti-dm/am62pxx

    cp ${WORKDIR}/rules-${MACHINE} ${S}/debian/rules

    sed -ni '/### TI extra config/q;p' ${S}/configs/${U_BOOT_CONFIG}
    cat ${WORKDIR}/ti-extra.cfg >> ${S}/configs/${U_BOOT_CONFIG}
}
