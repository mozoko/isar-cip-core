#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2021 - 2026
#
# Authors:
#  Quirin Gylstorff <quirin.gylstorff@siemens.com>
#
# SPDX-License-Identifier: MIT

inherit dpkg-raw

MAINTAINER = "cip-dev <cip-dev@lists.cip-project.org>"
DESCRIPTION = "Lua round-robin SWUpdate handler"

PROVIDES = "swupdate-handlers"

SRC_URI += "git://gitlab.com/cip-project/cip-sw-updates/swupdate-handler-roundrobin.git;protocol=https;destsuffix=swupdate-handler-roundrobin;name=swupdate-handler-roundrobin;nobranch=1"
SRCREV_swupdate-handler-roundrobin ?= "105ba14dca6fbf168292a1182e8c1e18a728bcd4"
SWUPDATE_LUASCRIPT = "swupdate-handler-roundrobin/swupdate_handlers_roundrobin.lua"

SWUPDATE_BOOTLOADER ??= "efibootguard"
SWUPDATE_ROUND_ROBIN_HANDLER_CONFIG ?= "${@ 'swupdate.handler.efibootguard.ini' if d.getVar('SWUPDATE_BOOTLOADER') == 'efibootguard' else ''}"
SRC_URI += "${@('file://' + d.getVar('SWUPDATE_ROUND_ROBIN_HANDLER_CONFIG')) if d.getVar('SWUPDATE_ROUND_ROBIN_HANDLER_CONFIG') else ''}"

# The lua version used by swupdate to search for additional handler is hard coded in debian/rules
# see https://salsa.debian.org/debian/swupdate/-/blob/6ccd44a8539ebb880bf0dac408d5db5de7e2de99/debian/rules#L13

SWUPDATE_LUA_VERSION = "5.5"
SWUPDATE_LUA_VERSION:trixie = "5.4"
SWUPDATE_LUA_VERSION:bookworm = "5.3"
SWUPDATE_LUA_VERSION:bullseye = "5.3"
SWUPDATE_LUA_VERSION:buster = "5.3"

SWUPDATE_ADDITIONAL_LUA_VERSIONS ??= "5.3"

do_prepare_build:append() {
    for lua_version in ${SWUPDATE_ADDITIONAL_LUA_VERSIONS}; do
        echo "usr/share/lua/${SWUPDATE_LUA_VERSION} usr/share/lua/$lua_version" >> ${WORKDIR}/${PN}-${PV}/debian/${PN}.links
    done
}

do_install[cleandirs] = "${D}/etc \
                         ${D}/usr/share/lua/${SWUPDATE_LUA_VERSION}"
do_install() {
    if [ -e ${WORKDIR}/${SWUPDATE_LUASCRIPT} ]; then
        install -m 0644 ${WORKDIR}/${SWUPDATE_LUASCRIPT} ${D}/usr/share/lua/${SWUPDATE_LUA_VERSION}/swupdate_handlers.lua
    fi
    if [ -n "${SWUPDATE_ROUND_ROBIN_HANDLER_CONFIG}" ] && [ -e ${WORKDIR}/${SWUPDATE_ROUND_ROBIN_HANDLER_CONFIG} ]; then
        install -m 0644 ${WORKDIR}/${SWUPDATE_ROUND_ROBIN_HANDLER_CONFIG} ${D}/etc/swupdate.handler.ini
    fi
}
