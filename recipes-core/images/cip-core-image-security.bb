#
# A reference image which includes security packages
#
# Copyright (c) Toshiba Corporation, 2020
#
# Authors:
#  Kazuhiro Hayashi <kazuhiro3.hayashi@toshiba.co.jp>
#
# SPDX-License-Identifier: MIT
#

require cip-core-image.inc

DESCRIPTION = "CIP Core image including security packages"

IMAGE_INSTALL += "security-customizations"
IMAGE_INSTALL += "fail2ban-config"

# Debian packages that provide security features
IMAGE_PREINSTALL += " \
	openssl \
	openssh-server openssh-sftp-server openssh-client \
	aide \
	nftables \
	libpam-pkcs11 \
	chrony \
	tpm2-tools \
	acl \
	audispd-plugins \
	uuid-runtime \
	sudo \
	aide-common \
	passwd \
	login \
	util-linux \
	apache2 \
	curl \
"

CIP_IMAGE_OPTIONS ?= ""
require ${CIP_IMAGE_OPTIONS}
