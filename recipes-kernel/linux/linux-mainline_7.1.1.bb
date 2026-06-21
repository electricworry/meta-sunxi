require linux-mainline.inc

DESCRIPTION = "Mainline Longterm Linux kernel"

LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"

SRC_URI[sha256sum] = "5215fa3541dc7e7f5bcd51bf7e57f169cec6fce508ca54e3dc85fdee14371d7d"

LINUX_VERSION = "7.1.1"

SRC_URI:append:orange-pi-zero3  = " \
    file://defconfig \
"

ERROR_QA:remove = "patch-status-core patch-status-noncore patch-status obsolete-license"
WARN_QA:remove = "patch-status-core patch-status-noncore patch-status obsolete-license"
