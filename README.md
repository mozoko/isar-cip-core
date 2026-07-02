# CIP Core (Generic Profile) Demonstration and Test Images

This generates bootable images for virtual and physical targets using the
Debian package set of the [CIP](https://www.cip-project.org/) Core Generic
Profile and the CIP SLTS kernel.

The build system used for this is [Isar](https://github.com/ilbers/isar), an
image generator that assembles Debian binaries or builds individual packages
from scratch.

## Building Target Images

Install docker or podman. For true unprivileged builds, podman is recommended.
See [kas documentation](https://kas.readthedocs.io/en/latest/userguide/kas-container.html)
for more details.

Open up the image configuration menu and select the desired target and its
options:

    ./kas-container menu

You can directly start the build from the menu.

If you prefer selecting the configuration via the command line, this builds
the BeagleBone Black target image with real-time kernel, e.g.:

    ./kas-container build kas-cip.yml:kas/board/bbb.yml:kas/opt/rt.yml


## Running Target Images

When having built a virtual QEMU target image, this can be started directly.
Run, e.g.,

    ./start-qemu.sh x86

when having built a QEMU AMD64 image. Using the image configuration menu will
initialize variables used by start-qemu.sh with fitting defaults. Otherwise, a
security image for QEMU can be started like this:

    TARGET_IMAGE=cip-core-image-security ./start-qemu.sh x86

For using the Firmware TPM / UEFI key storage option with a virtual target
(`kas/opt/ftpm-stmm.yml`), QEMU v10.2.0 or newer is required.

Physical targets will generate ready-to-boot images under
`build/tmp/deploy/images/`. To flash, e.g., the BeagleBone Black image to an SD
card, run

    dd if=build/tmp/deploy/images/bbb/cip-core-image-cip-core-buster-bbb.wic \
       of=/dev/<medium-device> bs=1M status=progress

or via bmap-tools

    bmaptool copy build/tmp/deploy/images/bbb/cip-core-image-cip-core-buster-bbb.wic /dev/<medium-device>

## Documentation

The following topics are in separate READMEs:
 - [Secureboot](./doc/README.secureboot.md)
 - [Update process with SWUpdate](./doc/README.swupdate.md)
 - [Update and Secure boot signing](./doc/README.signing.md)
 - [Encryption with TPM2](./doc/README.tpm2.encryption.md)
 - [Factory-reset](./doc/README.factory-reset.md)
 - [CIS hardening](./doc/README.cis-hardening.md)
 - [Release policy](./doc/README_version-release-policy.md)

## Community Resources

Mailing list:

 - cip-dev@lists.cip-project.org

 - Subscription:
   - cip-dev+subscribe@lists.cip-project.org
   - https://lists.cip-project.org/g/cip-dev

 - Archives:
   - https://lore.kernel.org/cip-dev/
   - https://lists.cip-project.org/g/cip-dev

Continuous integration:

  - https://gitlab.com/cip-project/cip-core/isar-cip-core/-/pipelines

Please see [CONTRIBUTING.md](CONTRIBUTING.md) for the contribution process.


## License

Unless otherwise stated in the respective file, files in this layer are
provided under the MIT license, see COPYING file. Patches (files ending with
.patch) are licensed according to their target project and file, typically
GPLv2.
