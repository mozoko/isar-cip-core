# Integrate isar-cip-core in an Isar layer

This document describes how to integrate isar-cip-core into a existing isar layer and build a
A/B root file system which can be updated with SWUpdate.

## Add cip-core to a kas.yaml

Add the isar-cip-core layer to a kas-file:
```yaml
  cip-core:
    url: https://gitlab.com/cip-project/cip-core/isar-cip-core.git
    branch: master
```

## Configure the root filesystem for A/B Updates

Add this line switches the root file system to a read-only file system
with /etc as overlay. It also installs and configures efibootguard and SWUpdate.
An additional image type is generated with contains the update artifacts.

```bitbake
require recipes-core/images/swupdate.inc
```

## Create a image description

Create a new wks file to describe the image. It is recommended to use
 [wic/qemu-amd64-efibootguard.wks.in](../wic/qemu-amd64-efibootguard.wks.in) as
 the base for this image. As all necessary variables are set.

## Set variables

Generate the uuid for the root file system partitions. These variables are used
by SWUpdate to select the update partition.
```
ABROOTFS_PART_UUID_A = "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"
ABROOTFS_PART_UUID_B = "YYYYYYYY-YYYY-YYYY-YYYY-YYYYYYYYYYYY"
```

Set the certificate source and mechanism to sign the update artifact.

```
PREFERRED_PROVIDER_swupdate-certificates-key = "swupdate-certificates-key-snakeoil"
PREFERRED_PROVIDER_swupdate-certificates = "swupdate-certificates-snakeoil"
PREFERRED_PROVIDER_swupdate-signer = "swupdate-signer-cms"
```

Set  the variable `OVERRIDES` to `:swupdate` to customize the initramfs:
```
OVERRIDES .= ":swupdate"
```
