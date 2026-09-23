#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2022-2026
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

DESCRIPTION = "Standalone Management Mode from EDK2"

inherit dpkg

SRC_URI = " \
    https://github.com/tianocore/edk2/archive/refs/tags/edk2-stable${PV}.tar.gz;subdir=${S} \
    https://github.com/tianocore/edk2-platforms/archive/${SRCREV-edk2-platforms}.tar.gz;name=edk2-platforms;downloadfilename=edk2-platforms-${SRCREV-edk2-platforms}.tar.gz;subdir=${S} \
    https://github.com/google/brotli/archive/${SRCREV-brotli}.tar.gz;name=brotli;downloadfilename=brotli-${SRCREV-brotli}.tar.gz;subdir=${S} \
    https://github.com/MIPI-Alliance/public-mipi-sys-t/archive/${SRCREV-mipisyst}.tar.gz;name=mipisyst;downloadfilename=mipisyst-${SRCREV-mipisyst}.tar.gz;subdir=${S} \
    https://github.com/openssl/openssl/archive/refs/tags/${PV-openssl}.tar.gz;name=openssl;subdir=${S} \
    https://github.com/Mbed-TLS/mbedtls/archive/${PV-mbedtls}.tar.gz;name=mbedtls;subdir=${S} \
    https://github.com/DMTF/libspdm/archive/${SRCREV-libspdm}.tar.gz;name=libspdm;downloadfilename=libspdm-${SRCREV-libspdm}.tar.gz;subdir=${S} \
    file://0001-Limit-pointer-constraint-to-clang.patch;patchdir=${S}/mbedtls-${PV-mbedtls} \
    file://rules \
    "
SRC_URI[sha256sum] = "bac40812213576c3025cf780c33d882f9e087a44201b1cd28a07f6d1eb54b76a"
SRC_URI[edk2-platforms.sha256sum] = "c58167ae3f2c1787c781b74777d2084276b982ea8ad0f517a46a05f31925ccf7"
SRC_URI[brotli.sha256sum] = "a9ba3940267de5dd73581a47c2e81b3eb1e1df6a704138c599020d66f3677a92"
SRC_URI[mipisyst.sha256sum] = "9fda3b9a78343ab2be6f06ce6396536e7e065abac29b47c8eb2e42cbb4c4f00b"
SRC_URI[openssl.sha256sum] = "d71a811bfbd9153d7b30cbe476263302ee4b04a9a47ffea6e6a782326805c93f"
SRC_URI[mbedtls.sha256sum] = "de654c64688d0f6c71caf7336010e1fb54070e6e3d1df3999ee432ab59d0dbde"
SRC_URI[libspdm.sha256sum] = "a815aa38874d2c82255f379a7ee1eb07cccbb439a7788ff14316fce52d61a516"

# according to edk2 submodules
SRCREV-brotli = "e230f474b87134e8c6c85b630084c612057f253e"
SRCREV-mipisyst = "370b5944c046bab043dd8b133727b2135af7747a"
SRCREV-libspdm = "f55cf6d48ec69b4ac60a63903e9c6a2cb0fd155d"

# revision closest to edk2 release
SRCREV-edk2-platforms = "f8dbb5c603302dc661f572b560bdb34f7db6edb5"

PV-openssl = "openssl-3.5.7"
PV-mbedtls = "mbedtls-3.6.6"

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
    ln -s ../../../../libspdm-${SRCREV-libspdm} ${S}/edk2/SecurityPkg/DeviceSecurity/SpdmLib/libspdm

    echo "Build/MmStandaloneRpmb/RELEASE_GCC/FV/BL32_AP_MM.fd /usr/lib/edk2/" > \
        ${S}/debian/edk2-standalonemm-rpmb.install
}
