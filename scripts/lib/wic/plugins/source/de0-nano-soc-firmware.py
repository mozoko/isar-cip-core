#
# Copyright (c) Siemens AG, 2025
#
# SPDX-License-Identifier: MIT

import logging

from wic import WicError
from wic.pluginbase import SourcePlugin
from wic.misc import exec_native_cmd

logger = logging.getLogger('wic')

class DE0NanoSoCFirmware(SourcePlugin):
    """
    Create hybrid partition table with a single bootable partition.
    """

    name = 'de0-nano-soc-firmware'

    @classmethod
    def do_install_disk(cls, disk, disk_name, creator, workdir, oe_builddir,
                        bootimg_dir, kernel_dir, native_sysroot):
        for part in creator.parts:
            if part.mbr:
                break
        else:
            raise WicError("No active partition found")

        logger.info("Marking MBR partition %d as firmware partition" % part.realnum)
        exec_native_cmd("sfdisk --label-nested dos --part-type %s %d 0xa2" % (disk.path, part.realnum), native_sysroot)
