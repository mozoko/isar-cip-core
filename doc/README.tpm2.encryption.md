# Encrypted Partitions

By adding the recipe `initramfs-crypt-hook` to the initramfs build user defined partitions will be
encrypted during first boot. The encrypted partition is a LUKS partition and uses a TPM to secure the
passphrase on the device.

> :exclamation:**IMPORTANT**
> All selected partitions are encrypted on first boot. In order to avoid the leakage of secrets
> the disk encryption must occur in a secure environment.

## Requirements

Testing with qemu-amd64 requires the package `swtpm`. Under Debian/Ubuntu this can be installed

```shell
apt-get install swtpm
```

## TPM2 protected LUKS passphrase

The recipe `initramfs-crypt-hook` uses `systemd-cryptenroll` (Debian 12 and later)
or `clevis` (Debian 10 and Debian 11) to enroll a TPM2 protected LUKS passphrase.
The procedure for storing a key is described in [systemd/src/shared/tpm2-util.c](https://github.com/systemd/systemd/blob/0254e4d66af7aa893b31b2326335ded5dde48b51/src/shared/tpm2-util.c#L1395).

## How to build an QEMU image with TPM encryption

An example for qemu-amd64 can be build with by selecting the option after calling:

```
./kas-container menu
```

or by adding using the following command line build:

```
./kas-container build kas-cip.yml:kas/board/qemu-amd64.yml:kas/opt/ebg-swu.yml:kas/opt/encrypt-data.yml
```

## initramfs-crypt-hook configuration

The initramfs-crypt-hook recipe has the following variables which can be overwritten during image build:

- CRYPT_PARTITIONS
- CRYPT_CREATE_FILE_SYSTEM_CMD

### CRYPT_PARTITIONS

The variable `CRYPT_PARTITIONS` contains the information which partition shall be encrypted where to mount it.
Each entry uses the schema `<partition-identifier>:<mountpoint>:<reencrypt | format | noencrypt | format-if-empty>`.

- The `partition-idenitifer` is used to identify the partition on the disk, it can contain a partition label, partition UUID or absolute path to the partition device, e.g. `/dev/sda`.
- The `mountpoint` is used mount the decrypted partition in the root file system
- `reencrypt` uses `cryptsetup reencrypt` to encrypt the exiting content of the partition. This reduces the partition by 32MB and the file system by a similar amount
- `format` creates a empty LUKS partition and creates a file system defined with the shell command given in `CRYPT_CREATE_FILE_SYSTEM_CMD`
- `noencrypt` will not try to encrypt the partition if it isn't encrypted already, but will open it if it is. See the section [Encrypting the shared partition via an update](#### Encrypting the shared partition via an update) for more information
- `format-if-empty` will create an empty LUKS partition and format it, like the `format` option, but only if the first 10 MiB are empty (contain only 0x00). This makes it possible to differentiate if a partition is empty and can be encrypted, because it was freshly flashed via a factory image, or if it might contain an unencrypted fallback system and should be left alone.

Power failure safety: Changes to the partition structure or file system super blocks can be dangerous and may result in the system failing to boot if power is cut at a critical point. Efforts were made to either eliminate these critical periods or keep them as short as possible. However, some remain. For example, if writing the LUKS header block is interrupted halfway through, the script and tools may misinterpret the state and be unable to recover.

#### Encrypted root file system

To encrypt the root file system the variable `CRYPT_PARTITIONS` needs to be set to:

```
CRYPT_PARTITIONS = "${ABROOTFS_PART_UUID_A}::reencrypt ${ABROOTFS_PART_UUID_B}::reencrypt"
```

The mountpoint is empty as the root partition is mounted by a seperate initramfs hook.
Both partitions are encrypted during first boot. The initramfs hook opens `${ABROOTFS_PART_UUID_A}` and `${ABROOTFS_PART_UUID_B}`
during boot.

#### Encrypting the shared partition via an update

With the following requirements, special handling is necessary:

- A/B update scheme is used.
- Both slots have a shared volume that needs to be encrypted as well.
- The system in the field is currently unencrypted, and encryption should be added via an update.
- When the update fails, the fallback system needs to deal with an encrypted data partition.

In this case, the fallback system needs to support an encrypted shared data partition but would not encrypt it on its own. For this, the `noencrypt` flag can be used.

The data partition in the fallback system will have the `noencrypt` flag set, while the update system will set the flag to `reencrypt`. This will handle the following case:

- Unencrypted system on slot A is running; the shared data partition has set the `noencrypt` flag and is not encrypted.
- Update for enabling encryption is applied to slot B, where the shared data partition has the `reencrypt` flag.
- System reboots to slot B, encrypting the shared data partition.
- Update fails at a later point and is not blessed; system reboots into the fallback system on slot A.
- Fallback system now needs to be able to use the shared data partition.

Example:

```mermaid
flowchart LR

    S1["Step 1: Field State (v1)<br/>Slot A: Active<br/>Flag: none<br/>Slot B: Inactive<br/>Flag: none<br/>Shared Data: Unencrypted"]

    S2["Step 2: Apply SWUpdate (v2 with noencrypt)<br/>Slot A: Inactive<br/>Flag: none<br/>Slot B: Active<br/>Flag: noencrypt<br/>Shared Data: Unencrypted"]

    S3["Step 3: Apply SWUpdate (v3 with reencrypt)<br/>Slot A: Inactive<br/>Flag: reencrypt<br/>Slot B: Active<br/>Flag: noencrypt<br/>Shared Data: Unencrypted"]

    SUCCESSFUL["Case 1: Applied successfully <br/> <br/>Slot A: Active<br/>Flag: reencrypt<br/>Slot B: Inactive<br/>Flag: noencrypt<br/>Shared Data: Encrypted"]

    UNSUCCESSFUL["Case 2: Failure after encrypting shared partitions <br/> <br/> Rollback to Slot B<br/>Slot A: Inactive<br/>Flag: reencrypt<br/>Slot B: Active<br/>Flag: noencrypt<br/>Shared Data: Encrypted"]

    OK["noencrypt flag prevents re-encryption<br/>  <br/> Encrypted data remains<br/>accessible"]

    S1 --> S2
    S2 --> S3
    S3 --> SUCCESSFUL
    S3 --> UNSUCCESSFUL
    UNSUCCESSFUL --> OK
```

* v1 (field state): cannot decrypt
* v2 (intermediate update): could decrypt, but must not encrypt so that rollback to v1 still work
* v3 (final update): can decrypt, thus encrypts, and rollback to v2 would still be possible

In this case, where encryption is added via an update, the `format-if-empty` option is also useful. The system with encryption enabled has the `format-if-empty` option set for the partition(s) in the inactive update slot. This will cause both sets of partitions in both slots to be encrypted after the first boot on a fresh factory flashed system, but will not disturb existing data of any fallback system if booted after an update.

### CRYPT_CREATE_FILE_SYSTEM_CMD

The variable `CRYPT_CREATE_FILE_SYSTEM_CMD` contains the command to create a new file system on a newly
encrypted partition. The Default (`mke2fs -t ext4`) creates an ext4 partition.

# Convert clevis based encryption to systemd-cryptenroll

## Prerequisites

The following packages are necessary to convert a clevis based encryption to a systemd-cryptenroll
based encryption:

- clevis-luks
- clevis-tpm2
- cryptsetup
- jq

## steps to convert clevis to systemd

The following script shows how to enroll a systemd-tpm2 token with a existing clevis based encryption:

```bash
export device=/dev/sda6
export keyslot=$(sudo cryptsetup luksDump "$device" --dump-json-metadata | jq -c '.tokens.[] | select( .type == "clevis") | .keyslots | first' | head -n1)
if [ -n "$keyslot" ]; then
  export PASSWORD=$(clevis luks pass -d "$device" -s"$keyslot")
  systemd-cryptenroll --tpm2-device="$tpm_device" --tpm2-pcrs=7 "$device"
fi
```

# TPM2 based encryption on generic x86

For a generic x86 platform with TPM2 module the build can be started with:

```bash
kas-container menu
```

The TPM2 module should support:

- a sha256 pcr bar with the ecc algorithm.

If only a sha1 pcr bar is avaiable the variable `CRYPT_HASH_TYPE` needs to be set to `sha1`.
