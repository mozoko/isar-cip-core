#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2020-2024
#
# Authors:
#  Christian Storm <christian.storm@siemens.com>
#  Quirin Gylstorff <quirin.gylstorff@siemens.com>
#  Felix Moessbauer <felix.moessbauer@siemens.com>
#
# SPDX-License-Identifier: MIT

inherit template

RO_ROOTFS_TYPE ??= "squashfs"
SWU_ROOTFS_TYPE ?= "${RO_ROOTFS_TYPE}"
SWU_ROOTFS_NAME ?= "${IMAGE_FULLNAME}"
SWU_KERNEL_NAME ?= "linux.efi"
# compression type as defined by swupdate (zlib or zstd). Set to empty string to disable compression
SWU_COMPRESSION_TYPE ?= "zlib"
SWU_ROOTFS_PARTITION_NAME ?= "${SWU_ROOTFS_NAME}.${SWU_ROOTFS_TYPE}${@get_swu_compression_type(d)}"
SWU_VERSION ?= "0.2"
SWU_NAME ?= "cip software update"
# space separated list of supported hw. Leave empty to leave out
SWU_HW_COMPAT ??= ""

SWU_EBG_UPDATE ?= ""
SWU_EFI_BOOT_DEVICE ?= "/dev/disk/by-uuid/4321-DCBA"
SWU_BOOTLOADER ??= "ebg"
SWU_DESCRIPITION_FILE_BOOTLOADER ??= "${SWU_DESCRIPTION_FILE}-${SWU_BOOTLOADER}"
SWU_DELTA_UPDATE_ARTIFACT = "${SWU_ROOTFS_NAME}.delta_update${@get_swu_compression_type(d)}"
SWU_ROOTFS_ARTIFACT_NAME = "${@ '${SWU_DELTA_UPDATE_ARTIFACT}' \
    if d.getVar('DELTA_UPDATE_TYPE') == "rdiff" or d.getVar('DELTA_UPDATE_TYPE') == "zchunk" \
    else '${SWU_ROOTFS_PARTITION_NAME}'}"

SWU_DELTA_UPDATE_KERNEL_ARTIFACT = "${SWU_KERNEL_NAME}.delta_update"
SWU_KERNEL_ARTIFACT_NAME = "${@ '${SWU_DELTA_UPDATE_KERNEL_ARTIFACT}' \
    if d.getVar('DELTA_UPDATE_TYPE') == "rdiff" \
    else '${SWU_KERNEL_NAME}'}"

SWU_IMAGE_FILE ?= "${IMAGE_FULLNAME}"
SWU_DESCRIPTION_FILE ?= "sw-description"
SWU_ADDITIONAL_FILES ?= "${SWU_KERNEL_ARTIFACT_NAME} ${SWU_ROOTFS_ARTIFACT_NAME}"
SWU_SIGNED ??= ""
SWU_SIGNATURE_EXT ?= "sig"
SWU_SIGNATURE_TYPE ?= "cms"

SWU_BUILDCHROOT_IMAGE_FILE ?= "${@os.path.basename(d.getVar('SWU_IMAGE_FILE'))}"

SWU_UPDATE_ARTIFACT_TYPE = "${SWU_ROOTFS_TYPE}${@get_swu_compression_type(d)}"
SWU_DELTA_UPDATE_ARTIFACT_TYPE = "delta_update${@get_swu_compression_type(d)}"
IMAGE_TYPEDEP:swu = "${@ '${SWU_DELTA_UPDATE_ARTIFACT_TYPE}' \
    if d.getVar('DELTA_UPDATE_TYPE') == "rdiff" or d.getVar('DELTA_UPDATE_TYPE') == "zchunk" \
    else '${SWU_UPDATE_ARTIFACT_TYPE}' }"

IMAGER_INSTALL:swu += "cpio"
IMAGE_INSTALL += "${@'swupdate-certificates' if bb.utils.to_boolean(d.getVar('SWU_SIGNED')) else ''}"


IMAGE_SRC_URI:swu = "file://${SWU_DESCRIPTION_FILE}.tmpl"
IMAGE_SRC_URI:swu += "file://${SWU_DESCRIPITION_FILE_BOOTLOADER}.tmpl"
IMAGE_TEMPLATE_FILES:swu = "${SWU_DESCRIPTION_FILE}.tmpl"
IMAGE_TEMPLATE_FILES:swu += "${SWU_DESCRIPITION_FILE_BOOTLOADER}.tmpl"
IMAGE_TEMPLATE_VARS:swu = " \
    RO_ROOTFS_TYPE \
    SWU_ROOTFS_ARTIFACT_NAME \
    SWU_KERNEL_ARTIFACT_NAME \
    TARGET_IMAGE_UUID \
    ABROOTFS_PART_UUID_A \
    ABROOTFS_PART_UUID_B \
    SWU_HW_COMPAT_NODE \
    SWU_COMPRESSION_NODE \
    SWU_VERSION \
    SWU_NAME \
    SWU_FILE_NODES \
    SWU_BOOTLOADER_FILE_NODE \
    SWU_SCRIPTS_NODE \
    SWU_DELTA_UPDATE_ROOTFS_PROPERTIES \
    SWU_DELTA_UPDATE_KERNEL_PROPERTIES \
    "

# TARGET_IMAGE_UUID needs to be generated before completing the template
addtask transform_template after do_generate_image_uuid

do_extend_sw_description[vardeps] += "SWU_HW_COMPAT"
python do_extend_sw_description() {
    cmds = d.getVar("SWU_EXTEND_SW_DESCRIPTION")
    if cmds is None or not cmds.strip():
        return
    cmds = cmds.split()
    for cmd in cmds:
        bb.build.exec_func(cmd, d)
}
do_transform_template[prefuncs] += "do_extend_sw_description"

SWU_EXTEND_SW_DESCRIPTION += "add_swu_hw_compat"
python add_swu_hw_compat() {
    # create SWU_HW_COMPAT_NODE based on list of supported hw
    hw_compat = d.getVar('SWU_HW_COMPAT')
    if hw_compat:
        hw_entries = ', '. join(['"' + h + '"' for h in hw_compat.split()])
        d.setVar('SWU_HW_COMPAT_NODE',
            'hardware-compatibility: [ ' + hw_entries +' ];')
    else:
        d.setVar('SWU_HW_COMPAT_NODE', '')
}

SWU_EXTEND_SW_DESCRIPTION += "add_swu_compression"
python add_swu_compression() {
    # create SWU_COMPRESSION_NODE node if compression is enabled
    calgo = d.getVar('SWU_COMPRESSION_TYPE')
    if calgo:
        d.setVar('SWU_COMPRESSION_NODE', 'compressed = "' + calgo + '";')
    else:
        d.setVar('SWU_COMPRESSION_NODE', '')
}

def add_scripts_to_src_uri(d):
    swu_scripts = d.getVar('SWU_SCRIPTS')
    if not swu_scripts:
        return ""
    swu_script_entries = swu_scripts.split()
    script_file_list = []
    for entry in swu_script_entries:
        script_entry = f"SWU_SCRIPT_{entry}"
        script_file = d.getVarFlag(script_entry, "file")
        script_file_list.append(f" file://{script_file}")
    return ' '.join([n for n in script_file_list])

SRC_URI += "${@add_scripts_to_src_uri(d)}"

SWU_EXTEND_SW_DESCRIPTION += "add_scripts_node"
python add_scripts_node() {
    swu_scripts = d.getVar('SWU_SCRIPTS')
    if not swu_scripts:
        return
    swu_script_entries = swu_scripts.split()
    script_node_list = []
    for entry in swu_script_entries:
        script_entry = f"SWU_SCRIPT_{entry}"

        script_file = d.getVarFlag(script_entry, "file")
        if not script_file:
            bb.warn(f"flag 'file' is empty for {script_entry} ")
            continue

        script_type = d.getVarFlag(script_entry, "type") or None
        allowed_script_types = [None, "lua", "shellscript", "preinstall", "postinstall"]
        if script_type not in allowed_script_types:
            bb.warn(f"flag 'type' is not of value {allowed_script_types} ")
            continue

        script_data = d.getVarFlag(script_entry, "data")
        node = f"""
        {{
          filename = "{script_file}";
        """
        if script_type:
            node += f"""  type = "{script_type}";"""
        if script_data:
            node += f"""  data = "{script_data}";"""
        node += f"""
          sha256 = "{script_file}-sha256";
        }}"""
        script_node_list.append(node)

    swu_scripts_node = "scripts: (" + ','.join([n for n in script_node_list]) + ");"
    d.appendVar('SWU_SCRIPTS_NODE', swu_scripts_node)
}

SWU_EXTEND_SW_DESCRIPTION += "add_swu_delta_update_properties"
python add_swu_delta_update_properties() {
    delta_type = d.getVar('DELTA_UPDATE_TYPE')
    swu_delta_update_rootfs_properties = ""
    swu_delta_update_kernel_properties = ""
    if delta_type == "rdiff":
        swu_delta_update_rootfs_properties =  'chainhandler = "rdiff_image";'
        swu_delta_update_kernel_properties =  'chainhandler = "rdiff_file";'
    elif delta_type == "zchunk":
        zck_url = d.getVar('DELTA_ZCK_URL')
        swu_delta_update_rootfs_properties = f"""
                        chainhandler = "delta";
                        url = "{zck_url}";
                        zckloglevel = "error";
        """
    d.setVar('SWU_DELTA_UPDATE_ROOTFS_PROPERTIES', swu_delta_update_rootfs_properties)
    d.setVar('SWU_DELTA_UPDATE_KERNEL_PROPERTIES', swu_delta_update_kernel_properties)

}

# convert between swupdate compressor name and imagetype extension
def get_swu_compression_type(d):
    swu_ct = d.getVar('SWU_COMPRESSION_TYPE')
    if not swu_ct:
        return ''
    swu_to_image = {'zlib': '.gz', 'zstd': '.zst'}
    if swu_ct not in swu_to_image:
        bb.fatal('requested SWU_COMPRESSION_TYPE is not supported by swupdate')
    return swu_to_image[swu_ct]

# This imagetype is neither machine nor distro specific. Hence, we cannot
# use paths in FILESOVERRIDES. Manual modifications of this variable are
# discouradged and hard to implement. Instead, we register this path explicitly.
# We append to the path, so locally provided config files are preferred
FILESEXTRAPATHS:append = ":${LAYERDIR_cip-core}/recipes-core/images/swu"

do_image_swu[depends] += "${PN}:do_transform_template"
do_image_swu[stamp-extra-info] = "${DISTRO}-${MACHINE}"
do_image_swu[cleandirs] += "${WORKDIR}/swu ${WORKDIR}/swu-${SWU_BOOTLOADER}"
do_image_swu[prefuncs] = "do_extend_sw_description"
IMAGE_CMD:swu() {
    rm -f '${DEPLOY_DIR_IMAGE}/${SWU_IMAGE_FILE}'*.swu
    cp '${WORKDIR}/${SWU_DESCRIPTION_FILE}' '${WORKDIR}/swu/${SWU_DESCRIPTION_FILE}'
    if [ -f '${WORKDIR}/${SWU_DESCRIPITION_FILE_BOOTLOADER}' ]; then
        cp '${WORKDIR}/${SWU_DESCRIPITION_FILE_BOOTLOADER}' '${WORKDIR}/swu-${SWU_BOOTLOADER}/${SWU_DESCRIPTION_FILE}'
    fi

    for swu_file in "${WORKDIR}"/swu*; do
        swu_file_base=$(basename $swu_file)
        # Create symlinks for files used in the update image
        swu_files=$(awk '$1=="filename"{gsub(/[",;]/, "", $3); print $3}' \
            "${WORKDIR}/$swu_file_base/${SWU_DESCRIPTION_FILE}" | sort | uniq)
        export swu_files
        for file in $swu_files; do
            if [ -e "${WORKDIR}/$file" ]; then
                ln -s "${PP_WORK}/$file" "${WORKDIR}/$swu_file_base/$file"
            else
                ln -s "${PP_DEPLOY}/$file" "${WORKDIR}/$swu_file_base/$file"
            fi
        done

        # Prepare for signing
        export sign='${@'x' if bb.utils.to_boolean(d.getVar('SWU_SIGNED')) else ''}'
        export swu_file_base
        # create a exetension to differ between swus
        swu_file_extension=""
        if [ "$swu_file_base" != "swu" ]; then
            swu_file_extension=${swu_file_base#swu}
        fi
        export swu_file_extension
        imager_run -p -d ${PP_WORK} -u root <<'EOIMAGER'
            # Fill in file check sums
            for file in $swu_files; do
                sed -i "s:$file-sha256:$(sha256sum "${PP_WORK}/$swu_file_base/"$file | cut -f 1 -d " "):g" \
                    "${PP_WORK}/$swu_file_base/${SWU_DESCRIPTION_FILE}"
            done
            cd "${PP_WORK}/$swu_file_base"
            cpio_files="${SWU_DESCRIPTION_FILE}"

            if [ -n "$sign" ]; then
                if [ ! -x /usr/bin/sign-swu ]; then
                    echo "Could not find the executable '/usr/bin/sign-swu'" 1>&2
                    exit 1
                fi
                signature_file="${SWU_DESCRIPTION_FILE}.${SWU_SIGNATURE_EXT}"
                if ! /usr/bin/sign-swu "${SWU_DESCRIPTION_FILE}" "$signature_file" > /dev/null 2>&1 || \
                        [ ! -f "$signature_file" ]; then
                    echo "Could not create swupdate signature file '$signature_file'" 1>&2
                    exit 1
                fi
                cpio_files="$cpio_files $signature_file"
            fi
            # check if swu_files are less than 4GBytes.
            # This avoids the limit of cpio
            for swu_file in $swu_files; do
                file_size=$(stat -c %s "$swu_file")
                if [ "$file_size" -ge 4294967295 ] ; then
                    echo "The size of '$swu_file': '$file_size' is greater" \
                          "than the limit of the swu format of 4294967295" \
                          "Bytes per file" 1>&2
                    exit 1
                fi
            done
            # sw-description must be first file in *.swu
            for cpio_file in $cpio_files $swu_files; do
                if [ -f "$cpio_file" ]; then
                    # Set file timestamps for reproducible builds
                    if [ -n "${SOURCE_DATE_EPOCH}" ]; then
                        touch -d@"${SOURCE_DATE_EPOCH}" "$cpio_file"
                    fi
                    case "$cpio_file" in
                        sw-description*)
                            echo "$cpio_file"
                            ;;
                        *)
                            if grep -q "$cpio_file" \
                                    "${WORKDIR}/$swu_file_base/${SWU_DESCRIPTION_FILE}"; then
                                echo "$cpio_file"
                            fi
                            ;;
                    esac
                fi
            done | cpio \
                --verbose --dereference --create --reproducible --format=crc \
                > "${PP_DEPLOY}/${SWU_IMAGE_FILE}$swu_file_extension.swu"
EOIMAGER
    done
}

python do_check_swu_partition_uuids() {
    for u in ['A', 'B']:
        if not d.getVar('ABROOTFS_PART_UUID_' + u):
            bb.fatal('ABROOTFS_PART_UUID_' + u + ' not set')
}

addtask check_swu_partition_uuids before do_image_swu
