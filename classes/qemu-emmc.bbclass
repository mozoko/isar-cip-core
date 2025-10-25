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

IMAGE_TYPEDEP:qemu_emmc = "wic"

SOURCE_IMAGE_FILE ?= "${IMAGE_FULLNAME}.wic"
SOURCE_IMAGE_PATH = "${DEPLOY_DIR_IMAGE}/${SOURCE_IMAGE_FILE}"

QEMU_EMMC_RPMB_PART_SIZE ?= "2097152"

python set_data_part_size() {
    import os

    size = os.stat(d.getVar('SOURCE_IMAGE_PATH')).st_size
    if size > 2 * 1024 * 1024 * 1024:
        size = (size + 511) & ~511
    elif size & (size - 1) > 0:
        n = 0
        while size > 0:
            size >>= 1
            n = n + 1
        size = 1 << n
    size += int(d.getVar('QEMU_EMMC_RPMB_PART_SIZE'))
    d.setVar('QEMU_EMMC_IMAGE_SIZE', str(size))
}

do_image_qemu_emmc[prefuncs] = "set_data_part_size"
IMAGE_CMD:qemu_emmc() {
    dd if=/dev/zero of="${IMAGE_FILE_HOST}" status=none bs=128K \
        count=${@int(d.getVar('QEMU_EMMC_RPMB_PART_SIZE')) // (128*1024)}
    cat "${SOURCE_IMAGE_PATH}" >> "${IMAGE_FILE_HOST}"

    image_size=$(stat -L -c %s "${SOURCE_IMAGE_PATH}")
    truncate "${IMAGE_FILE_HOST}" -s ${QEMU_EMMC_IMAGE_SIZE}
}
