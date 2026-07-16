#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2021-2024
#
# Authors:
#  Quirin Gylstorff <quirin.gylstorff@siemens.com>
#
# SPDX-License-Identifier: MIT
#

IMAGE_TYPEDEP:verity = "${VERITY_IMAGE_TYPE}"
IMAGER_INSTALL:verity += "cryptsetup"

RO_ROOTFS_TYPE ??= "squashfs"
VERITY_IMAGE_TYPE ?= "${RO_ROOTFS_TYPE}"
VERITY_INPUT_IMAGE ?= "${IMAGE_FULLNAME}.${VERITY_IMAGE_TYPE}"
VERITY_OUTPUT_IMAGE ?= "${IMAGE_FULLNAME}.verity"
VERITY_IMAGE_METADATA = "${VERITY_OUTPUT_IMAGE}.metadata"
VERITY_HASH_BLOCK_SIZE ?= "1024"
VERITY_DATA_BLOCK_SIZE ?= "1024"

# Set the salt used to generate a verity image to a fixed value
# if not set it is derived from TARGET_IMAGE_UUID
VERITY_IMAGE_SALT ?= ""

# Set the UUID used to generate a verity image to a fixed value
# if not set it is set to TARGET_IMAGE_UUID
VERITY_IMAGE_UUID ?= ""

python derive_verity_salt_and_uuid() {
    import hashlib

    verity_salt = d.getVar("VERITY_IMAGE_SALT")
    verity_uuid = d.getVar("VERITY_IMAGE_UUID")
    target_uuid = d.getVar("TARGET_IMAGE_UUID")

    if not verity_salt:
        if target_uuid:
            verity_salt = hashlib.sha256(target_uuid.encode()).hexdigest()
        else:
            bb.error("TARGET_IMAGE_UUID and VERITY_IMAGE_SALT are empty. Could not set VERITY_SALT.")

    if not verity_uuid:
        if target_uuid:
            verity_uuid = target_uuid
        else:
            bb.error("TARGET_IMAGE_UUID and VERITY_IMAGE_UUID are empty. Could not set VERITY_UUID.")

    d.setVar("VERITY_IMAGE_SALT_OPTION", "--salt=" + str(verity_salt))
    d.setVar("VERITY_IMAGE_UUID_OPTION", "--uuid=" + str(verity_uuid))
}

create_verity_env_file() {

    local ENV="${DEPLOY_DIR_IMAGE}/${IMAGE_FULLNAME}.verity.env"
    rm -f $ENV

    local input="${WORKDIR}/${VERITY_IMAGE_METADATA}"
    # remove header from verity meta data
    sed -i '/VERITY header information for/d' $input
    IFS=":"
    while read KEY VAL; do
        printf '%s=%s\n' \
            "$(echo "$KEY" | tr '[:lower:]' '[:upper:]' | sed 's/ /_/g')" \
            "$(echo "$VAL" | tr -d ' \t' | sed -e 's/\[.*\]//' )" >> $ENV
    done < $input
}

python calculate_verity_data_blocks() {
    import os

    image_file = os.path.join(
        d.getVar("DEPLOY_DIR_IMAGE"),
        d.getVar("VERITY_INPUT_IMAGE")
    )
    data_block_size = int(d.getVar("VERITY_DATA_BLOCK_SIZE"))
    size = os.stat(image_file).st_size
    assert size % data_block_size == 0, f"image is not well-sized!"
    d.setVar("VERITY_INPUT_IMAGE_SIZE", str(size))
    d.setVar("VERITY_DATA_BLOCKS", str(size // data_block_size))
}

do_image_verity[vardeps] += "VERITY_IMAGE_UUID VERITY_IMAGE_SALT"
do_image_verity[cleandirs] = "${WORKDIR}/verity"
do_image_verity[prefuncs] = "calculate_verity_data_blocks derive_verity_salt_and_uuid"
IMAGE_CMD:verity() {
    rm -f ${DEPLOY_DIR_IMAGE}/${VERITY_OUTPUT_IMAGE}
    echo -n >${WORKDIR}/${VERITY_IMAGE_METADATA}

    cp -a ${DEPLOY_DIR_IMAGE}/${VERITY_INPUT_IMAGE} ${DEPLOY_DIR_IMAGE}/${VERITY_OUTPUT_IMAGE}

    ${SUDO_CHROOT} sh -c '/sbin/veritysetup format \
        --hash-block-size "${VERITY_HASH_BLOCK_SIZE}"  \
        --data-block-size "${VERITY_DATA_BLOCK_SIZE}"  \
        --data-blocks "${VERITY_DATA_BLOCKS}" \
        --hash-offset "${VERITY_INPUT_IMAGE_SIZE}" \
        "${VERITY_IMAGE_SALT_OPTION}" \
        "${VERITY_IMAGE_UUID_OPTION}" \
        "${PP_DEPLOY}/${VERITY_OUTPUT_IMAGE}" \
        "${PP_DEPLOY}/${VERITY_OUTPUT_IMAGE}" \
        >>"${PP_WORK}/${VERITY_IMAGE_METADATA}"'

    echo "Hash offset:    	${VERITY_INPUT_IMAGE_SIZE}" \
        >>"${WORKDIR}/${VERITY_IMAGE_METADATA}"
    create_verity_env_file
}
addtask image_verity after do_generate_image_uuid
