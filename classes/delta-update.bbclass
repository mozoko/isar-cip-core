#
# CIP Core, generic profile
#
# Copyright (c) Toshiba Corporation 2024
#
# Authors:
#  Adithya Balakumar <adithya.balakumar@toshiba-tsip.com>
#
# SPDX-License-Identifier: MIT
#

IMAGER_INSTALL:delta_update .= " zchunk rdiff"

FILESEXTRAPATHS:prepend = "${TOPDIR}/previous-image:"

DELTA_UPDATE_TYPE ??= ""
DELTA_RDIFF_REF_IMAGE ??= ""
DELTA_RDIFF_REF_KERNEL_IMAGE ??= ""
DELTA_ZCK_URL ??= ""
DELTA_PREV_IMAGE_PATH ??= "${TOPDIR}/previous-image"

def disable_delta_update_tasks (d):
    d.appendVarFlag("do_image_delta_update", "noexec", "1")
    d.setVar("DELTA_UPDATE_TYPE", "")

python () {
    if d.getVar("DELTA_UPDATE_TYPE") == "rdiff":
        if d.getVar("DELTA_RDIFF_REF_IMAGE") == "" or d.getVar("DELTA_RDIFF_REF_KERNEL_IMAGE") == "":
            bb.fatal("You must set DELTA_RDIFF_REF_IMAGE and DELTA_RDIFF_REF_KERNEL_IMAGE and also provide the required files as artifacts to this recipe")
    elif d.getVar("DELTA_UPDATE_TYPE") == "zchunk":
        if d.getVar("BASE_DISTRO_CODENAME") != "trixie":
            bb.fatal("Zchunk based delta update is only supported from trixie onward")
    else:
        disable_delta_update_tasks(d)
}

python do_fetch_delta_rdiff_ref_image () {
    if d.getVar("DELTA_UPDATE_TYPE") == "rdiff":
        rootfs_path = d.getVar("DELTA_PREV_IMAGE_PATH") + "/" + d.getVar("DELTA_RDIFF_REF_IMAGE")
        kernel_path = d.getVar("DELTA_PREV_IMAGE_PATH") + "/" + d.getVar("DELTA_RDIFF_REF_KERNEL_IMAGE")
        if not os.path.isfile(rootfs_path) or not os.path.isfile(kernel_path):
            bb.fatal("No such files found: "+ rootfs_path + "," + kernel_path + ". Provide the required files at "+ d.getVar("DELTA_PREV_IMAGE_PATH") + " for rdiff based delta update")
        else:
            d.appendVar("SRC_URI", " file://" + d.getVar("DELTA_RDIFF_REF_IMAGE"))
            d.appendVar("SRC_URI", " file://" + d.getVar("DELTA_RDIFF_REF_KERNEL_IMAGE"))
}

do_fetch[prefuncs] += "do_fetch_delta_rdiff_ref_image"
do_unpack[prefuncs] += "do_fetch_delta_rdiff_ref_image"

create_rdiff_delta_artifact() {
    rm -f ${DEPLOY_DIR_IMAGE}/${IMAGE_FULLNAME}.delta
    # create signature file with rdiff for rootfs
    ${SUDO_CHROOT} /usr/bin/rdiff signature ${WORKDIR}/${DELTA_RDIFF_REF_IMAGE} \
        ${WORKDIR}/delta_interim_artifacts/old-image-rootfs.sig

    # create delta file with the signature file for rootfs
    ${SUDO_CHROOT} /usr/bin/rdiff delta ${WORKDIR}/delta_interim_artifacts/old-image-rootfs.sig \
        ${PP_DEPLOY}/${IMAGE_FULLNAME}.${SWU_ROOTFS_TYPE} ${PP_DEPLOY}/${IMAGE_FULLNAME}.delta

    DELTA_ARTIFACT_ROOTFS_SWU=${IMAGE_FULLNAME}.delta

    # create a symbolic link as IMAGE_CMD expects a *.delta_update file in deploy image directory
    ln -sf ${DELTA_ARTIFACT_ROOTFS_SWU} ${DEPLOY_DIR_IMAGE}/${IMAGE_FULLNAME}.delta_update

    rm -f ${DEPLOY_DIR_IMAGE}/linux.efi.delta

    # create signature file with rdiff for kernel
    ${SUDO_CHROOT} /usr/bin/rdiff signature ${WORKDIR}/${DELTA_RDIFF_REF_KERNEL_IMAGE} \
        ${WORKDIR}/delta_interim_artifacts/old-linux.efi.sig

    # create delta file with the signature file for kernel
    ${SUDO_CHROOT} /usr/bin/rdiff delta ${WORKDIR}/delta_interim_artifacts/old-linux.efi.sig \
        ${PP_DEPLOY}/linux.efi ${PP_DEPLOY}/linux.efi.delta

    ln -sf linux.efi.delta ${DEPLOY_DIR_IMAGE}/linux.efi.delta_update
}

create_zchunk_delta_artifact() {
    # Create .zck file
    ${SUDO_CHROOT} /bin/zck \
        --output ${PP_DEPLOY}/${IMAGE_FULLNAME}.zck \
        -u --chunk-hash-type sha256 \
        ${PP_DEPLOY}/${IMAGE_FULLNAME}.${SWU_ROOTFS_TYPE}

    # Calculate size of zck header
    HSIZE="$(${SUDO_CHROOT} /bin/zck_read_header -v ${PP_DEPLOY}/${IMAGE_FULLNAME}.zck | grep "Header size" | cut -d ':' -f 2)"

    # Extract the zck header
    ${SUDO_CHROOT} /bin/dd if="${PP_DEPLOY}/${IMAGE_FULLNAME}".zck of="${PP_DEPLOY}/${IMAGE_FULLNAME}".zck.header bs=1 count="$HSIZE" status=none

    DELTA_ARTIFACT_SWU=${IMAGE_FULLNAME}.zck.header

    # create a symbolic link as IMAGE_CMD expects a *.delta_update file in deploy image directory
    ln -sf ${DELTA_ARTIFACT_SWU} ${DEPLOY_DIR_IMAGE}/${IMAGE_FULLNAME}.delta_update
}

do_image_delta_update[cleandirs] += "${WORKDIR}/delta_interim_artifacts"
do_image_delta_update[depends] += "${PN}:do_transform_template"
IMAGE_CMD:delta_update() {
    case "${DELTA_UPDATE_TYPE}" in
    "rdiff")
        create_rdiff_delta_artifact
        ;;
    "zchunk")
        create_zchunk_delta_artifact
        ;;
    *)
        bbfatal "You must set a valid DELTA_UPDATE_TYPE (rdiff/zchunk)"
        ;;
    esac
}

addtask image_delta_update before do_image_swu after do_image_wic
