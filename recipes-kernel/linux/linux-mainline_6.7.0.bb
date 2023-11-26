require linux-mainline.inc

DESCRIPTION = "Mainline Longterm Linux kernel"

LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"

SRC_URI = "https://git.kernel.org/torvalds/t/linux-6.7-rc2.tar.gz"

SRC_URI[sha256sum] = "f09555bd1617112ff20807fb67866732ad65045c74cf24075cce1d424cc475d1"


SRC_URI:append:orange-pi-zero3  = " \
        file://defconfig \
"

S = "${WORKDIR}/linux-6.7-rc2"
LINUX_VERSION ?= "6.7.0"
KERNEL_VERSION_SANITY_SKIP="1"
