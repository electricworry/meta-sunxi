SECTION = "kernel"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"
COMPATIBLE_MACHINE = "(sun4i|sun5i|sun7i|sun8i|sun50i)"

inherit kernel

require linux.inc

LINUX_VERSION = "6.11"
PV = "${LINUX_VERSION}+git"

KERNEL_DTB_PREFIX = "0"

DEPENDS += "rsync-native coreutils-native"

# Pull in the devicetree files into the rootfs
RDEPENDS_${KERNEL_PACKAGE_NAME}-base += "kernel-devicetree"

KERNEL_EXTRA_ARGS += "LOADADDR=${UBOOT_ENTRYPOINT}"

S = "${WORKDIR}/linux-${PV}"

SRC_URI = "git://github.com/electricworry/linux-sunxi;branch=master;protocol=https \
	   file://defconfig \
"
SRCREV = "${AUTOREV}"

SRC_URI:append:use-mailine-graphics = " file://drm.cfg"

FILES_${KERNEL_PACKAGE_NAME}-base:append = " ${nonarch_base_libdir}/modules/${KERNEL_VERSION}/modules.builtin.modinfo"

KERNEL_VERSION_SANITY_SKIP = "1"
