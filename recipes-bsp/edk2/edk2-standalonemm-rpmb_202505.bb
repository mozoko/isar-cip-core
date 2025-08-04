#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2022-2025
#
# Authors:
#  Sven Schultschik <sven.schultschik@siemens.com>
#  Jan Kiszka <jan.kiszka@siemens.com>
#  Li Hua Qian <huaqian.li@siemens.com>
#
# SPDX-License-Identifier: MIT
#

HOMEPAGE = "https://github.com/tianocore/edk2"
MAINTAINER = "Sven Schultschik <sven.schultschik@siemens.com>"

inherit dpkg

SRC_URI = " \
    https://github.com/tianocore/edk2/archive/refs/tags/edk2-stable${PV}.tar.gz;subdir=${S} \
    https://github.com/tianocore/edk2-platforms/archive/${SRCREV-edk2-platforms}.tar.gz;name=edk2-platforms;subdir=${S} \
    https://github.com/google/brotli/archive/${SRCREV-brotli}.tar.gz;name=brotli;subdir=${S} \
    https://github.com/MIPI-Alliance/public-mipi-sys-t/archive/${SRCREV-mipisyst}.tar.gz;name=mipisyst;subdir=${S} \
    https://github.com/openssl/openssl/archive/refs/tags/${PV-openssl}.tar.gz;name=openssl;subdir=${S} \
    https://github.com/Mbed-TLS/mbedtls/archive/${PV-mbedtls}.tar.gz;name=mbedtls;subdir=${S} \
    https://github.com/DMTF/libspdm/archive/refs/tags/${PV-libspdm}.tar.gz;name=libspdm;subdir=${S} \
    file://rules \
    "
SRC_URI[sha256sum] = "5f2b5e3a267230f82e4566592fd0bfac5e205ad90520b2c9bf80f575293b7015"
SRC_URI[edk2-platforms.sha256sum] = "0a266a61732d8dbb95bd2a4c0bf64a91965728b9fcd567af6af0894f23da0f97"
SRC_URI[brotli.sha256sum] = "aaa739962a45b508b2e783b915e6b2b57ed3b12bd4b0feac73acfb144dffa54f"
SRC_URI[mipisyst.sha256sum] = "9fda3b9a78343ab2be6f06ce6396536e7e065abac29b47c8eb2e42cbb4c4f00b"
SRC_URI[openssl.sha256sum] = "d4b22527a645acf76b53e44487a8db687c6eed621d7246891d025e38ba8c9651"
SRC_URI[mbedtls.sha256sum] = "a22ff38512697b9cd8472faa2ea2d35e320657f6d268def3a64765548b81c3ec"
SRC_URI[libspdm.sha256sum] = "dfadf501d23c26041c921974971953a1d4a250ed6cd2679facb781471a5f944c"

# according to edk2 submodules
SRCREV-brotli = "ed738e842d2fbdf2d6459e39267a633c4a9b2f5d"
SRCREV-mipisyst = "370b5944c046bab043dd8b133727b2135af7747a"

# revision closest to edk2 release
SRCREV-edk2-platforms = "cf63a7257a0a12044b4530904835427813855f4f"

PV-openssl = "openssl-3.4.1"
PV-mbedtls = "mbedtls-3.3.0"
PV-libspdm = "3.6.0"


DEBIAN_BUILD_DEPENDS = "bash, python3:native, dh-python, uuid-dev:native"

# edk2-edk2-stable comes with two pre-built .a files that need to be preserved
DPKG_SOURCE_EXTRA_ARGS = ""

do_prepare_build() {
    deb_debianize

    ln -sf edk2-edk2-stable${PV} ${S}/edk2
    ln -sf edk2-platforms-${SRCREV-edk2-platforms} ${S}/edk2-platforms

    rm -rf ${S}/edk2/BaseTools/Source/C/BrotliCompress/brotli
    ln -s ../../../../../brotli-${SRCREV-brotli} ${S}/edk2/BaseTools/Source/C/BrotliCompress/brotli

    rm -rf ${S}/edk2/MdeModulePkg/Library/BrotliCustomDecompressLib/brotli
    ln -s ../../../../brotli-${SRCREV-brotli} ${S}/edk2/MdeModulePkg/Library/BrotliCustomDecompressLib/brotli

    rm -rf ${S}/edk2/MdePkg/Library/MipiSysTLib/mipisyst
    ln -s ../../../../public-mipi-sys-t-${SRCREV-mipisyst} ${S}/edk2/MdePkg/Library/MipiSysTLib/mipisyst

    rm -rf ${S}/edk2/CryptoPkg/Library/OpensslLib/openssl
    ln -s ../../../../openssl-${PV-openssl} ${S}/edk2/CryptoPkg/Library/OpensslLib/openssl

    rm -rf ${S}/edk2/CryptoPkg/Library/MbedTlsLib/mbedtls
    ln -s ../../../../mbedtls-${PV-mbedtls} ${S}/edk2/CryptoPkg/Library/MbedTlsLib/mbedtls

    rm -rf ${S}/edk2/SecurityPkg/DeviceSecurity/SpdmLib/libspdm
    ln -s ../../../../libspdm-${PV-libspdm} ${S}/edk2/SecurityPkg/DeviceSecurity/SpdmLib/libspdm

    echo "Build/MmStandaloneRpmb/RELEASE_GCC5/FV/BL32_AP_MM.fd /usr/lib/edk2/" > \
        ${S}/debian/edk2-standalonemm-rpmb.install
}
