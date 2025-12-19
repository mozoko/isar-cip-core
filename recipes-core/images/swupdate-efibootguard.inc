#
# CIP Core, generic profile
#
# Copyright (c) Siemens AG, 2024 - 2026
#
# Authors:
#  Quirin Gylstorff <quirin.gylstorff@siemens.com>
#
# SPDX-License-Identifier: MIT
#

IMAGE_INSTALL:append = " efibootguard-tools libebgenv0"
IMAGER_INSTALL:wic:append = " efibootguard:${DISTRO_ARCH} efibootguard-tools:${DISTRO_ARCH}"
WDOG_TIMEOUT ?= "60"
WICVARS += "WDOG_TIMEOUT KERNEL_IMAGE INITRD_DEPLOY_FILE DTB_FILES EFI_ARCH EFI_LIB_ARCH UKI_FILENAME"
IMAGE_TYPEDEP:swu:append = " wic"

def distro_to_efi_arch(d):
    DISTRO_TO_EFI_ARCH = {
        "amd64": "x64",
        "arm64": "aa64",
        "armhf": "arm",
        "i386": "ia32",
        "riscv64": "riscv64"
    }
    distro_arch = d.getVar('DISTRO_ARCH')
    return DISTRO_TO_EFI_ARCH[distro_arch]

EFI_ARCH := "${@distro_to_efi_arch(d)}"

def distro_to_lib_arch(d):
    DISTRO_TO_LIB_ARCH = {
        "amd64": "x86_64-linux-gnu",
        "arm64": "aarch64-linux-gnu",
        "armhf": "arm-linux-gnueabihf",
        "i386": "i386-linux-gnu",
        "riscv64": "riscv64-linux-gnu",
    }
    distro_arch = d.getVar('DISTRO_ARCH')
    return DISTRO_TO_LIB_ARCH[distro_arch]

EFI_LIB_ARCH := "${@distro_to_lib_arch(d)}"

# Add the bootloader file
def efi_bootloader_name(d):
    efi_arch = distro_to_efi_arch(d)
    return "boot{}.efi".format(efi_arch)

SWU_EXTEND_SW_DESCRIPTION += "add_ebg_update"
python add_ebg_update() {
    efi_boot_loader_file = efi_bootloader_name(d)
    efi_boot_device = d.getVar('SWU_EFI_BOOT_DEVICE')
    swu_ebg_update_node = f"""
    {{
        filename = "{efi_boot_loader_file}";
        path = "EFI/BOOT/{efi_boot_loader_file}";
        device = "{efi_boot_device}";
        filesystem = "vfat";
        sha256 = "{efi_boot_loader_file}-sha256";
        properties: {{
            atomic-install = "true";
        }};
    }}
    """

    d.setVar('SWU_BOOTLOADER_FILE_NODE', swu_ebg_update_node)
    ebg_update = d.getVar('SWU_EBG_UPDATE') or ""
    if ebg_update:
        d.appendVar('SWU_FILE_NODES', "," + swu_ebg_update_node)
}
