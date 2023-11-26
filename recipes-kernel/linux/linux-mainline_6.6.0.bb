require linux-mainline.inc

DESCRIPTION = "Mainline Longterm Linux kernel"

LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"

SRC_URI = "git://github.com/jernejsk/linux-1.git;protocol=https;branch=h616-var"

SRCREV  = "32d78aa3b6aa47abdb05b032977b6be3afcc3971"

SRC_URI:append:orange-pi-zero3  = " \
        file://defconfig \
"

LINUX_VERSION ?= "6.6.0"
KERNEL_VERSION_SANITY_SKIP="1"
