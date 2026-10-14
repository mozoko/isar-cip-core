# EFI Boot Guard secure boot

This document describes how to generate a secure boot capable image with
[efibootguard](https://github.com/siemens/efibootguard).

## Description

The image build signs the EFI Boot Guard bootloader (bootx64.efi) and generates
a signed [unified kernel image](https://uapi-group.org/specifications/specs/unified_kernel_image).
A unified kernel image packs the kernel, initramfs and the kernel command-line
in one binary object. As the kernel command-line is immutable after the build
process, the previous selection of the root file system with a command-line parameter is no longer
possible. Therefore the selection of the root file-system occurs now in the initramfs.

The image uses an A/B partition layout to update the root file system. The sample implementation to
select the root file system generates a uuid and stores the id in /etc/os-release and in the initramfs.
During boot the initramfs compares its own uuid with the uuid stored in /etc/os-release of each rootfs.
If a match is found the rootfs is used for the boot.

## Adaptation for Images

### WIC
The following elements must be present in a wks file to create a secure boot capable image.

```
part --source efibootguard-efi  --sourceparams "signwith=<script or executable to sign the image>"
part --source efibootguard-boot --sourceparams "signwith=<script or executable to sign the image>"
```

#### Script or executable to sign the image

The wic plugins for the [bootloader](./scripts/lib/wic/plugins/source/efibootguard-efi.py)
and [boot partition](./scripts/lib/wic/plugins/source/efibootguard-boot.py) require an
executable or script with the following interface:
```
<script_name> <inputfile> <outputfile>
```
- script name: name and path of the script added with
`--sourceparams "signwith=/usr/bin/sign_secure_image.sh"` to the wic image
- inputfile: path and name of the file to be signed
- outputfile: path and name of the signed input

Supply the script name and path to wic by adding
`signwith=<path and name of the script to sign>"` to sourceparams of the partition.

### Existing key packages for signing an image

#### secure-boot-snakeoil

This package uses the snakeoil key and certificate from the ovmf package from
Debian trixie or later for signing the image.

#### secure-boot-key

This package takes a user-generated certificate and key adds them to the build system.
The following variable and steps are necessary to build a secure boot capable image:
- Set certification information to sign and verify the image with:
    - SB_CERT: The certificate to verify the signing process
    - SB_KEY: The private key of for the certificate

The files referred by SB_CERT and SB_KEY must be store in `recipes-devtools/secure-boot-secrets/files/`.

## Running in QEMU

Set up a secure boot test environment with [QEMU](https://www.qemu.org/)

### Prerequisites

- OVMF from edk2 release 2025.02-8+deb13u1 or newer
  - This documentation was tested under Debian 13 with OVMF (2025.02-8+deb13u1) from Debian trixie
- efitools for KeyTool.efi
  - This documentation was tested under Debian 13 with efitools (1.9.2-3.5) from Debian trixie
- libnss3-tools

### Debian Snakeoil keys

The build copies the  Debian Snakeoil keys to the directory `./build/tmp/deploy/images/<machine>/OVMF.
You can use them as described in section [Start Image](#start-the-image).

### Generate Keys

#### Reuse existing keys

It is possible to use exiting keys like /usr/share/ovmf/PkKek-1-snakeoil.pem' from Debian
by executing the script  `scripts/generate-sb-db-from-existing-certificate.sh`, e.g.:
```
export SB_NAME=<name for the secureboot config>
export SB_KEYDIR=<location to store the database>
export INKEY=<secret key of the certificate>
export INCERT=<certificate>
export INNICK=<name of the certificate in the database>
scripts/generate-sb-db-from-existing-certificate.sh
```
This will create the directory `SB_KEYDIR` and will store the `${SB_NAME}certdb` with the given name.

Copy the used certificate and private key to `recipes-devtools/secure-boot-secrets/files/`

#### Generate keys

To generate the necessary keys and information to test secure-boot with QEMU
execute the script `scripts/generate_secure_boot_keys.sh`

##### Add Keys to OVMF
1. Create a folder and copy the generated keys and KeyTool.efi
(in Debian the file can be found at: /lib/efitools/x86_64-linux-gnu/KeyTool.efi) to the folder

```bash
mkdir secureboot-tools
cp -r keys secureboot-tools
cp /lib/efitools/x86_64-linux-gnu/KeyTool.efi secureboot-tools
```

2. Copy the file OVMF_VARS_4M.fd (in Debian the file can be found at /usr/share/OVMF/OVMF_VARS_4M.fd)
to the current directory. OVMF_VARS_4M.fd contains no keys can be instrumented for secureboot.
3. Start QEMU with the script scripts/start-efishell.sh

```bash
./scripts/start-efishell.sh secureboot-tools
```

4. Start the KeyTool.efi FS0:\KeyTool.efi and execute the the following steps:

```
          -> "Edit Keys"
             -> "The Allowed Signatures Database (db)"
                -> "Add New Key"
                -> Change/Confirm device
                -> Select "demoDB.esl" file
             -> "The Key Exchange Key Database (KEK)"
                -> "Add New Key"
                -> Change/Confirm device
                -> Select "demoKEK.esl" file
             -> "The Platform Key (PK)
                -> "Replace Key(s)"
                -> Change/Confirm device
                -> Select "demoPK.auth" file
```

5. quit QEMU

### Build image

Build the image with a signed EFI Boot Guard and unified kernel image
with the snakeoil keys by executing:

```bash
./kas-container build kas-cip.yml:kas/board/qemu-amd64.yml:kas/opt/trixie.yml:kas/opt/ebg-secure-boot-snakeoil.yml
```

For user-generated keys, create a new option file in the repository. This option file could look like this:

```yaml
header:
  version: 23
  includes:
   - kas/opt/ebg-swu.yml

local_conf_header:
  secure-boot-image: |
    IMAGE_FSTYPES = "wic"
    IMAGE_TYPEDEP:wic += "verity"
    SWU_ROOTFS_TYPE = "verity"
    WKS_FILE = "${MACHINE}-efibootguard-secureboot.wks.in"
    INITRAMFS_INSTALL:append = " initramfs-verity-hook"
    # abrootfs cannot be installed together with verity
    INITRAMFS_INSTALL:remove = "initramfs-abrootfs-hook"

  secure-boot: |
    IMAGER_BUILD_DEPS += "ebg-secure-boot-signer"
    IMAGER_INSTALL:wic += "ebg-secure-boot-signer"

    # Use user-generated keys
    PREFERRED_PROVIDER_secure-boot-secrets = "secure-boot-key"

  secureboot_override: |
    OVERRIDES .= ":secureboot"

  user-keys: |
    SB_CERT = "demoDB.crt"
    SB_KEY = "demoDB.key"
```

Replace `demo` with the name of the user-generated certificates. The user-generated certificates
need to stored in the folder `recipes-devtools/secure-boot-secrets/files`.

Build the image with user-generated keys by executing the command:

```bash
./kas-container build kas-cip.yml:kas/board/qemu-amd64.yml:kas/opt/trixie.yml:<path to the new option>.yml
```

### Start the image

#### Debian snakeoil

Start the image with the following command:

```bash
DISTRO_RELEASE=trixie SECURE_BOOT=y \
./start-qemu.sh amd64
```
#### User-generated keys
Start the image with the following command:

```bash
DISTRO_RELEASE=trixie SECURE_BOOT=y \
OVMF_CODE=./build/tmp/deploy/images/qemu-amd64/OVMF/OVMF_CODE_4M.secboot.fd \
OVMF_VARS=<path to the modified OVMF_VARS.fd> \
./start-qemu.sh amd64
```

After boot check the dmesg for secure boot status like below:

```bash
root@demo:~# dmesg | grep Secure
[    0.008368] Secure boot enabled
```

In case of arm64 or armhf architectures, the secure boot status can be found in bootloader logs like below:

```bash
EFI stub: UEFI Secure Boot is enabled.
```
## Example: Update the image

For updating the image, the following steps are necessary:
- [Build the image with snakeoil keys](#build-image)
- save the generated swu `build/tmp/deploy/images/qemu-amd64/cip-core-image-cip-core-trixie-qemu-amd64.swu` to /tmp
- modify the image for example, switch to the RT kernel as modification:

```bash
./kas-container build kas-cip.yml:kas/board/qemu-amd64.yml:kas/opt/trixie.yml:kas/opt/ebg-secure-boot-snakeoil.yml:kas/opt/rt.yml
```

- start the new target

```bash
DISTRO_RELEASE=trixie SECURE_BOOT=y \
./start-qemu.sh amd64
```

Copy the swu cip-core-image-cip-core-trixie-qemu-amd64.swu to the running system

```bash
scp -P 22222 /tmp/cip-core-image-cip-core-trixie-qemu-amd64.swu root@127.0.0.1:/
```

- check which is the root partition and it should be `/dev/sda4`

```bash
root@demo:~# ls -l /sys/block/dm-0/slaves/
total 0
lrwxrwxrwx 1 root root 0 Jun 30 12:56 sda4 -> ../../../../pci0000:00/0000:00:1f.2/ata6/host5/target5:0:0/5:0:0:0/block/sda/sda4
```

- install the swupdate and reboot the image

```bash
root@demo:~# swupdate -i cip-core-image-cip-core-trixie-qemu-amd64.swu
root@demo:~# reboot
```

- check which is the root partition now, the rootfs should have changed (i.e `/dev/sda5`):

```bash
root@demo:~# ls -l /sys/block/dm-0/slaves/
total 0
lrwxrwxrwx 1 root root 0 Jun 30 13:20 sda5 -> ../../../../pci0000:00/0000:00:1f.2/ata6/host5/target5:0:0/5:0:0:0/block/sda/sda5
```

## Secure boot on Generic UEFI x86

Secureboot for a generic UEFI x86 target works similar to the QEMU target,
except the enrollment of the secure boot keys.

### Generate keys from a single signing key

The recipe `single-key-to-efi-certs` can be used to generate a Debian package
that contains the required EFI certificates for enabling secure boot. It uses
a single key/cert pair as basis. Unless otherwise configured, this pair will
be taken from the corresponding Debian snakeoil set which is fine only for
testing purposes. The package will contain the `.auth` files required for
manual deployment:

```
$ dpkg -c build/tmp/deploy/isar-apt/cip-core-trixie/apt/cip-core-trixie/pool/main/s/single-key-to-efi-certs/single-key-to-efi-certs_0.1_arm64.deb
[...]
-rw-r--r-- root/root      2549 1970-01-01 02:00 ./usr/share/secure-boot-efi-certs/KEK.auth
-rw-r--r-- root/root      2549 1970-01-01 02:00 ./usr/share/secure-boot-efi-certs/PK.auth
-rw-r--r-- root/root      2549 1970-01-01 02:00 ./usr/share/secure-boot-efi-certs/db.auth
```

### Automatic secure boot certificate enrollment

By default, isar-cip-core images contain an early boot service
(initramfs-sbcerts-hook) which will deploy the required EFI certificates during
first boot and restart the machine into secure boot afterwards. This will only
happen when the machine is in Setup Mode and no other certificates have been
deployed to the Allowed Signature Database yet. This also ensures that disk
encryption on first boot will only take place when the machine was securely
booted so that the encryption key is sealed against this state in the TPM.

The certificates needed for this auto-enrollment are taken from a package that
provides `secure-boot-efi-certs`. The `single-key-to-efi-certs` package is the
default provider of this in isar-cip-core.

### Manual secure boot certificate enrollment

> :exclamation:**IMPORTANT** This document is not for generating a Machine Owner Key(MOK).

> :exclamation:**IMPORTANT** Enrollment of secure boot keys must occur
> in a secure environment.

The certificates for the following keys need to be enrolled onto the device:
 - The Platform Key (PK)
 - The Key Exchange Key (KEK)
 - Allowed Signatures Database

You can obtain them from the single-key-to-efi-certs package described before.

The enrollment can typically be achieved with the help of
[efi-updatevar](https://manpages.debian.org/bookworm/efitools/efi-updatevar.1.en.html)
on the device.

If the device supports built in EFI shell then the enrollment of keys can also be done by KeyTool.efi tool like below:

Format the USB memory stick

```
host$ sudo mkfs.vfat <usb device>
host$ sudo mount -t vfat /dev/<usb device> /mnt/
```

Copy the KeyTool.efi binary and self signed Secure Boot keys to USB stick

Here the folder "keys" contains Secure Boot keys(DB, KEK and PK).
```
host$ sudo apt install efitools
host$ sudo mkdir -p /mnt/efi/boot
host$ sudo cp /usr/lib/efitools/x86_64-linux-gnu/KeyTool.efi /mnt/efi/boot/KeyTool.efi
host$ sudo cp -r keys /mnt/
host$ sudo umount /mnt
```
Launch KeyTool.efi binary from the built in EFI shell and follow step-4 from the section [Add Keys to OVMF](#add-keys-to-ovmf) to inject Secure Boot keys. Otherwise, consult the manual of the specific UEFI Firmware.

Use the recipes [secure-boot-key](###secure-boot-key) to provided the keys
to the signing script contained in
[ebg-secure-boot-signer](###ebg-secure-boot-signer).

### [ebg-secure-boot-signer](./recipes-devtools/ebg-secure-boot-signer/ebg-secure-boot-signer_0.2.bb)

During building a efibootguard based wic image the scripts contained in
the recipe ebg-secure-boot-signer can be used to sign the bootloader and
unified kernel image(UKI). If the keys are stored in a HSM the script can
be exchanged to sign the artifacts in a more secure way.
