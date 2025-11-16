#!/bin/bash
# Copyright (C) 2024, Renesas Electronics Europe GmbH
# Chris Paterson <chris.paterson2@renesas.com>
# Sai Ashrith <sai.sathujoda@toshiba-tsip.com>
################################################################################

set -e

################################################################################
LAVA_TEMPLATES="tests/templates"
LAVA_JOBS_URL="https://${CIP_LAVA_LAB_SERVER:-lava.ciplatform.org}/scheduler/job"
LAVA_API_URL="https://$CIP_LAVA_LAB_USER:$CIP_LAVA_LAB_TOKEN@${CIP_LAVA_LAB_SERVER:-lava.ciplatform.org}/api/v0.2"
LAVACLI_ARGS="--uri https://$CIP_LAVA_LAB_USER:$CIP_LAVA_LAB_TOKEN@${CIP_LAVA_LAB_SERVER:-lava.ciplatform.org}/RPC2"
SQUAD_GROUP="cip-core"
SQUAD_WATCH_JOBS_URL="${CIP_SQUAD_URL}/api/watchjob"
SQUAD_LAVA_BACKEND="${CIP_SQUAD_LAVA_BACKEND:-cip}"
PROJECT_URL="https://s3.eu-central-1.amazonaws.com/download2.cip-project.org/cip-core"

WORK_DIR=$(pwd)
RESULTS_DIR="$WORK_DIR/results"
ERROR=false
TEST=$1
TARGET=$2
COMMIT_REF=$3
RELEASE=$4
COMMIT_BRANCH=$5
IEC_TEST_TIMEOUT_MINUTES=$6

# Export to replace the timeout variable declared in IEC template
export IEC_TEST_TIMEOUT_MINUTES

if [ -z "$SUBMIT_ONLY" ]; then SUBMIT_ONLY=false; fi

# Create a dictionary to handle image arguments based on architecture
declare -A image_args
image_args[qemu-amd64]="-cpu qemu64 -machine q35,accel=tcg -smp 4 -global ICH9-LPC.noreboot=off -device ide-hd,drive=disk -drive if=pflash,format=raw,unit=0,readonly=on,file=/usr/share/OVMF/OVMF_CODE_4M.secboot.fd -device virtio-net-pci,netdev=net -drive if=pflash,format=raw,file=/usr/share/OVMF/OVMF_VARS_4M.snakeoil.fd  -global ICH9-LPC.disable_s3=1 -global isa-fdc.driveA= -device tpm-tis,tpmdev=tpm0"
image_args[qemu-arm64]="-cpu cortex-a57 -machine virt -smp 4 -device virtio-serial-device -device virtconsole,chardev=con -chardev vc,id=con -device virtio-blk-device,drive=disk -device virtio-net-device,netdev=net -device tpm-tis-device,tpmdev=tpm0"
image_args[qemu-arm]="-cpu cortex-a15 -machine virt -smp 2 -device virtio-serial-device -device virtconsole,chardev=con -chardev vc,id=con -device virtio-blk-device,drive=disk -device virtio-net-device,netdev=net -device tpm-tis-device,tpmdev=tpm0"

set_up (){
	job_dir="$(mktemp -d)"
}

clean_up () {
	rm -rf "$job_dir"
}

# This method is called only for arm64 and arm targets while building job definitions
add_firmware_artifacts () {
	sed -i "s@#Firmware#@firmware:@g" "$1"
	sed -i "s@#Firmware_args#@image_arg: '-bios {firmware}'@g" "$1"
	sed -i "s@#Firmware_url#@url: ${PROJECT_URL}/${COMMIT_BRANCH}/${2}/firmware.bin@g" "$1"
}

# This method creates LAVA job definitions for QEMU amd64, arm64 and armhf
# The created job definitions test SWUpdate, Secureboot and IEC layer
create_job_qemu () {
	if [ "$1" = "IEC" ]; then
		cp $LAVA_TEMPLATES/IEC_template.yml "${job_dir}/${1}_${2}.yml"

	elif [ "$1" = "swupdate" ]; then
		cp $LAVA_TEMPLATES/swupdate_template.yml "${job_dir}/${1}_${2}.yml"
		sed -i "s@#updatestate#@2@g" "${job_dir}"/*.yml

	elif [ "$1" = "kernel-panic" ] || [ "$1" = "initramfs-crash" ]; then
		cp $LAVA_TEMPLATES/swupdate_template.yml "${job_dir}/${1}.yml"
		sed -i "s@software update testing@${1}_rollback_testing@g" "${job_dir}"/*.yml
		sed -i -e "s@#updatestate#@3@g" -e "s@) = 2@) = 3@g" "${job_dir}"/*.yml
		if [ "$1" = "kernel-panic" ]; then
			sed -i "s@kernel: C:BOOT1:linux.efi@Kernel panic - not syncing: sysrq triggered crash@g" "${job_dir}"/*.yml
			sed -i "s@#branch#@maintain-lava-artifact@g" "${job_dir}"/*.yml
		else
			sed -i "s@kernel: C:BOOT1:linux.efi@Can't open verity rootfs - continuing will lead to a broken trust chain!@g" "${job_dir}"/*.yml
			sed -i "s@echo software update is successful!!@dd if=/dev/urandom of=/dev/sda5 bs=512 count=1@g" "${job_dir}"/*.yml
		fi
	elif [ "$1" = "secure-boot-unsigned-kernel" ]; then
		cp $LAVA_TEMPLATES/secureboot_negative_test.yml "${job_dir}/${1}_unsigned_kernel_${2}.yml"
		cd $LAVA_TEMPLATES
		sed -e '/#POSTPROCESS_STEPS#/ {' -e 'r secureboot_unsigned_kernel_steps.yml' -e 'd' -e '}' -i "${job_dir}/${1}_unsigned_kernel_${2}.yml"
		cd -
		if [ "$2" = "qemu-amd64" ]; then
			sed -i "s@#END_MONITOR#@Access Denied@g" "${job_dir}/${1}_unsigned_kernel_${2}.yml"
			sed -i "s@#START_MONITOR#@Cannot load specified kernel image@g" "${job_dir}/${1}_unsigned_kernel_${2}.yml"
			sed -i "s@#ARTIFACT#@linux@g" "${job_dir}/${1}_unsigned_kernel_${2}.yml"
		fi

		if [ "$2" = "qemu-arm64" ] || [ "$2" = "qemu-arm" ]; then
			sed -i "s@sda@vda@g" "${job_dir}/${1}_unsigned_kernel_${2}.yml"
			sed -i "s@#END_MONITOR#@Application failed@g" "${job_dir}/${1}_unsigned_kernel_${2}.yml"
			sed -i "s@#START_MONITOR#@Image not authenticated@g" "${job_dir}/${1}_unsigned_kernel_${2}.yml"
			sed -i "s@#ARTIFACT#@linux@g" "${job_dir}/${1}_unsigned_kernel_${2}.yml"
		fi
	elif [ "$1" = "secure-boot-unsigned-bootloader" ]; then
		cp $LAVA_TEMPLATES/secureboot_negative_test.yml "${job_dir}/${1}_unsigned_bootloader_${2}.yml"
		cd $LAVA_TEMPLATES
		sed -e '/#POSTPROCESS_STEPS#/ {' -e 'r secureboot_unsigned_bootloader_steps.yml' -e 'd' -e '}' -i "${job_dir}/${1}_unsigned_bootloader_${2}.yml"
		cd -

		if [ "$2" = "qemu-amd64" ]; then
			sed -i "s@#END_MONITOR#@BdsDxe: failed to load Boot@g" "${job_dir}/${1}_unsigned_bootloader_${2}.yml"
			sed -i "s@#START_MONITOR#@Access Denied@g" "${job_dir}/${1}_unsigned_bootloader_${2}.yml"
			sed -i "s@#ARTIFACT#@bootloader@g" "${job_dir}/${1}_unsigned_bootloader_${2}.yml"
		fi

		if [ "$2" = "qemu-arm64" ] || [ "$2" = "qemu-arm" ]; then
			sed -i "s@sda@vda@g" "${job_dir}/${1}_unsigned_bootloader_${2}.yml"

			sed -i "s@#END_MONITOR#@EFI Boot failed!@g" "${job_dir}/${1}_unsigned_bootloader_${2}.yml"
			sed -i "s@#START_MONITOR#@Image not authenticated@g" "${job_dir}/${1}_unsigned_bootloader_${2}.yml"
			sed -i "s@#ARTIFACT#@bootloader@g" "${job_dir}/${1}_unsigned_bootloader_${2}.yml"
		fi

		if [ "$2" = "qemu-arm64" ]; then
			sed -i "s@bootx64.efi@bootaa64.efi@g" "${job_dir}/${1}_unsigned_bootloader_${2}.yml"
		fi
		if [ "$2" = "qemu-arm" ]; then
			sed -i "s@bootx64.efi@bootarm.efi@g" "${job_dir}/${1}_unsigned_bootloader_${2}.yml"
		fi
	elif [ "$1" = "secure-boot-corrupt-rootfs" ]; then
		cp $LAVA_TEMPLATES/secureboot_negative_test.yml "${job_dir}/${1}_corrupt_rootfs_${2}.yml"
		cd $LAVA_TEMPLATES
		sed -e '/#POSTPROCESS_STEPS#/ {' -e 'r secureboot_corrupt_rootfs_steps.yml' -e 'd' -e '}' -i "${job_dir}/${1}_corrupt_rootfs_${2}.yml"
		cd -

		sed -i "s@#END_MONITOR#@reboot: Restarting system with command 'dm-verity device corrupted'@g" "${job_dir}/${1}_corrupt_rootfs_${2}.yml"
		sed -i "s@#START_MONITOR#@EFI stub: UEFI Secure Boot is enabled.@g" "${job_dir}/${1}_corrupt_rootfs_${2}.yml"
		sed -i "s@#ARTIFACT#@rootfs@g" "${job_dir}/${1}_corrupt_rootfs_${2}.yml"

		if [ "$2" = "qemu-arm64" ]; then
			sed -i "s@bootx64.efi@bootaa64.efi@g" "${job_dir}/${1}_corrupt_rootfs_${2}.yml"
		fi
		if [ "$2" = "qemu-arm" ]; then
			sed -i "s@bootx64.efi@bootarm.efi@g" "${job_dir}/${1}_corrupt_rootfs_${2}.yml"
		fi
	elif [ "$1" = "secure-boot-mismatch-keys" ]; then
		if [ "$2" = "qemu-amd64" ]; then
			cp $LAVA_TEMPLATES/secureboot_negative_test.yml "${job_dir}/${1}_mismatch_keys_${2}.yml"

			sed -i "s@#END_MONITOR#@BdsDxe: failed to load Boot@g" "${job_dir}/${1}_mismatch_keys_${2}.yml"
			sed -i "s@#START_MONITOR#@Access Denied@g" "${job_dir}/${1}_mismatch_keys_${2}.yml"
			sed -i "s@#ARTIFACT#@keys@g" "${job_dir}/${1}_mismatch_keys_${2}.yml"
			sed -i "s@#POSTPROCESS_STEPS#@- echo 'no postprocess steps'@g" "${job_dir}/${1}_mismatch_keys_${2}.yml"
		fi
	elif [ "$1" = "swupdate-corrupt-swu" ]; then
		cp $LAVA_TEMPLATES/swupdate_negative_test.yml "${job_dir}/${1}_corrupt_swu_${2}.yml"
		cd $LAVA_TEMPLATES
		sed -e '/#TEST_BLOCK_STEPS#/ {' -e 'r swupdate_corrupt_swu_steps.yml' -e 'd' -e '}' -i "${job_dir}/${1}_corrupt_swu_${2}.yml"
		cd -
	elif [ "$1" = "swupdate-corrupt-swu-artifact" ]; then
		cp $LAVA_TEMPLATES/swupdate_negative_test.yml "${job_dir}/${1}_corrupt_swu_artifact_${2}.yml"
		cd $LAVA_TEMPLATES
		sed -e '/#TEST_BLOCK_STEPS#/ {' -e 'r swupdate_corrupt_swu_artifact_steps.yml' -e 'd' -e '}' -i "${job_dir}/${1}_corrupt_swu_artifact_${2}.yml"
		cd -
	elif [ "$1" = "swupdate-reboot-without-confirm" ]; then
		cp $LAVA_TEMPLATES/swupdate_template.yml "${job_dir}/${1}_reboot_without_confirm_${2}.yml"
		cd $LAVA_TEMPLATES
		sed -e '/#REBOOT_WITHOUT_CONFIRM_STEPS#/ {' -e 'r swupdate_reboot_without_confirm.yml' -e 'd' -e '}' -i "${job_dir}/${1}_reboot_without_confirm_${2}.yml"
		cd -
		sed -i "s@bg_setenv -c@echo No update confirm@g" "${job_dir}/${1}_reboot_without_confirm_${2}.yml"
	elif [ "$1" = "swupdate-apply-same-image-swu" ]; then
		cp $LAVA_TEMPLATES/swupdate_negative_test.yml "${job_dir}/${1}_same_uuid_${2}.yml"
		cd $LAVA_TEMPLATES
		sed -e '/#TEST_BLOCK_STEPS#/ {' -e 'r swupdate_same_uuid_steps.yml' -e 'd' -e '}' -i "${job_dir}/${1}_same_uuid_${2}.yml"
		cd -
	else
		cp $LAVA_TEMPLATES/secureboot_template.yml "${job_dir}/${1}_${2}.yml"
	fi

	if [ "$1" != "kernel-panic" ]; then
		sed -i "s@#branch#@${COMMIT_BRANCH}@g" "${job_dir}"/*.yml
	fi

	if [ "$2" != "qemu-amd64" ]; then
		add_firmware_artifacts "${job_dir}"/*.yml "$2"
	fi

	sed -i -e "s@#distribution#@${RELEASE}@g" -e "s@#project_url#@${PROJECT_URL}@g" "${job_dir}"/*.yml
	sed -i -e "s@#architecture#@${2}@g" -e "s@#imageargs#@${image_args[$2]}@g" "${job_dir}"/*.yml

	if [ "$1" = "secure-boot-mismatch-keys" ]; then
		if [ "${RELEASE}" = "trixie" ]; then
			KEYS_DISTRO=bookworm
		else
			KEYS_DISTRO=trixie
		fi
		sed -i "s@/usr/share/OVMF/OVMF_CODE_4M.secboot.fd@/root/keys/${KEYS_DISTRO}-ovmf/OVMF_CODE_4M.secboot.fd@g" "${job_dir}/${1}_mismatch_keys_${2}.yml"
		sed -i "s@/usr/share/OVMF/OVMF_VARS_4M.snakeoil.fd@/root/keys/${KEYS_DISTRO}-ovmf/OVMF_VARS_4M.snakeoil.fd@g" "${job_dir}/${1}_mismatch_keys_${2}.yml"
	fi

	# Target is recieved from gitlab job in form of qemu-"architecture"
	# In the template context field needs only architecture excepting the device type
	local arch
	arch=$(echo "$2" | cut -d '-' -f 2)
	sed -i "s@#context-architecture#@${arch}@g" "${job_dir}"/*.yml
}

# This method creates LAVA job definitions for M-COM-x86
# The created job definitions test SWUpdate, Secureboot and IEC layer
create_job_mcom () {
	cp $LAVA_TEMPLATES/M-COM-x86.yml "${job_dir}/${1}_${2}.yml"
	if [ "$1" = "IEC" ]; then
		grep -A 9 "# TEST_BLOCK" "$LAVA_TEMPLATES/$1_template.yml" >> "${job_dir}/${1}_${2}.yml"
	elif [ "$1" = "secure-boot" ]; then
		grep -A 1 "parameters" "$LAVA_TEMPLATES/secureboot_template.yml" >> "${job_dir}/${1}_${2}.yml"
	else
		# swupdate -d option does not work on M-COM, so .swu file is deployed to downloads
		grep -A 7 "deploy:" "${job_dir}/${1}_${2}.yml" > "${job_dir}/swupdate_deploy_download.yml"
		sed -i -e "s@flasher@downloads@g" -e "s@wic.xz@swu@g" "${job_dir}/swupdate_deploy_download.yml"
		sed -i -e "/actions/r ${job_dir}/swupdate_deploy_download.yml" "${job_dir}/${1}_${2}.yml"

		# Remove the deploy to download yml file once it is placed in the job definition
		rm "${job_dir}/swupdate_deploy_download.yml"

		# swupdate test action on M-COM is different from the test block used in QEMU
		cat $LAVA_TEMPLATES/swupdate-test-action-M-COM.yml | tee -a "${job_dir}/${1}_${2}.yml" > /dev/null
		grep -A 12 "# BOOT BLOCK" "$LAVA_TEMPLATES/M-COM-x86.yml" >> "${job_dir}/${1}_${2}.yml"
		grep -A 16 "# TEST BLOCK 2" "$LAVA_TEMPLATES/$1_template.yml" >> "${job_dir}/${1}_${2}.yml"
		sed -i -e "s@#updatestate#@2@g" -e "s@overlay-1.1.1.4@overlay-2.1.1.4@g" "${job_dir}/${1}_${2}.yml"
	fi
	sed -i -e "s@#test_function#@${1}@g" -e "s@#branch#@${COMMIT_BRANCH}@g" "${job_dir}/${1}_${2}.yml"
	sed -i -e "s@#distribution#@${RELEASE}@g" -e "s@#project_url#@${PROJECT_URL}@g" "${job_dir}/${1}_${2}.yml"
}

# This method attaches SQUAD watch job to the submitted LAVA job
# $1: LAVA Job ID
submit_squad_watch_job(){
# SQUAD watch job submission
	local ret
	if [ -z ${CIP_SQUAD_LAB_TOKEN+x} ]; then
		echo "SQUAD_LAB_TOKEN not found, omitting SQUAD results reporting!"
		return 0
	fi

	if [ "$TEST" = "swupdate" ] || [ "$TEST" = "kernel-panic" ] || [ "$TEST" = "initramfs-crash" ]; then
		SQUAD_PROJECT="swupdate-testing"
	elif [ "$TEST" = "secure-boot" ]; then
		SQUAD_PROJECT="secure-boot-testing"
	elif [ "$TEST" = "IEC" ]; then
		SQUAD_PROJECT="iec-layer-testing"
	else
		echo "Unable to host results in available CIP Core SQUAD projects"
		return 1
	fi

	local ENV="${SQUAD_PROJECT}_${TARGET}"
	local squad_url="$SQUAD_WATCH_JOBS_URL/${SQUAD_GROUP}/${SQUAD_PROJECT}/${COMMIT_REF}/${ENV}"
	ret=$(curl -s \
		--header "Authorization: token $CIP_SQUAD_LAB_TOKEN" \
		--form backend="$SQUAD_LAVA_BACKEND" \
		--form testjob_id="$1" \
		--form metadata='{"device": "'"${DEVICE}"'", "CI pipeline": "'"${CI_PIPELINE_URL}"'", "CI job": "'"${CI_JOB_URL}"'"}' \
		"$squad_url")

	if [[ $ret != [0-9]* ]]
	then
		echo "Something went wrong with SQUAD watch job submission. SQUAD returned:"
		echo "${ret}"
		echo "SQUAD URL: ${squad_url}"
		echo "SQUAD Backend: ${SQUAD_LAVA_BACKEND}"
		echo "LAVA Job Id: $1"
	else
		echo "SQUAD watch job submitted successfully as #${ret}."
	fi
}

# $1: Job definition file
submit_job() {
        # First check if respective device is online
	local job device ret device
	job=$1
	device=$(grep device_type "$job" | cut -d ":" -f 2 | awk '{$1=$1};1')
	if is_device_online "$device"; then
		echo "Submitting $1 to LAVA master..."
		# Catch error that occurs if invalid yaml file is submitted
		# shellcheck disable=2086
		ret=$(lavacli $LAVACLI_ARGS jobs submit "$1") || ERROR=true

		if [[ $ret != [0-9]* ]]
		then
			echo "Something went wrong with job submission. LAVA returned:"
			return 1
		else
			echo "Job submitted successfully as #${ret}."
			echo "URL: ${LAVA_JOBS_URL}/${ret}"

			local lavacli_output=${job_dir}/lavacli_output
			# shellcheck disable=2086
			lavacli $LAVACLI_ARGS jobs show "${ret}" \
				> "$lavacli_output"

			DEVICE=$(grep "device      :" "$lavacli_output" \
				| cut -d ":" -f 2 \
				| awk '{$1=$1};1')

			submit_squad_watch_job "${ret}"

			# shellcheck disable=2086
			lavacli $LAVACLI_ARGS jobs logs "${ret}"
			# shellcheck disable=2086
			lavacli $LAVACLI_ARGS results "${ret}"

			get_junit_test_results "$ret"
		fi
	else
		return 1
	fi
}

# $1: Device-type to search for
is_device_online () {
	local count
	local lavacli_output=${job_dir}/lavacli_output

	# Get list of all devices
	# shellcheck disable=2086
	lavacli $LAVACLI_ARGS devices list > "$lavacli_output"

	# Count the number of online devices
	count=$(grep "(${1})" "$lavacli_output" | grep -c "Good")
	echo "There are currently $count \"${1}\" devices online."

	if [ "$count" -gt 0 ]; then
		return 0
	fi
	return 1
}

# This method checks if the job is valid before submitting it later on.
validate_job () {
	local job
	job=$(find "${job_dir}"/*.yml)
	# shellcheck disable=2086
	if lavacli $LAVACLI_ARGS jobs validate "${job}"; then
		echo "$job is a valid definition"
		if ! submit_job $job; then
			clean_up
			exit 1
		fi
	else
		echo "$job is not a valid definition"
		return 1
	fi
	return 0
}

get_first_xml_attr_value() {
	file=${1}
	tag=${2}

	grep -m 1 -o "${tag}=\".*\"" "${file}" | cut -d\" -f2
}

get_junit_test_results () {
	mkdir -p "${RESULTS_DIR}"
	curl -s -o "${RESULTS_DIR}"/results_"$1".xml "${LAVA_API_URL}"/jobs/"$1"/junit/

	# change return code to generate a error in gitlab-ci if a test is failed
	if xmlstarlet sel -t  -v "/testsuites/testsuite/testcase[@name='job']/failure/@type" "${RESULTS_DIR}"/results_"$1".xml; then
		ERROR=true
	fi
}

set_up

if [[ $TARGET =~ "qemu" ]]; then
	create_job_qemu "$TEST" "$TARGET"
elif [[ $TARGET =~ "x86-uefi" ]]; then
	create_job_mcom "$TEST" "$TARGET"
else
	echo "Invalid target"
	exit 1
fi

# Only replace the IEC_TEST_TIMEOUT_MINUTES variable in template files
envsubst '$IEC_TEST_TIMEOUT_MINUTES' < ${job_dir}/*.yml > template.tmp && mv template.tmp ${job_dir}/*.yml

if ! validate_job; then
	clean_up
	exit 1
fi

clean_up

if $ERROR; then
	exit 1
fi
