DESCRIPTION = "Orange Pi Firmware"
SECTION = "firmware"
LICENSE = "CC0-1.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/${LICENSE};md5=0ceb3372c9595f0a8067e55da801e4a1"

SRCBRANCH = "master"
SRCREV  = "b2809d6c7a79ab874a91b84b9b0d9169cb41a749"

SRC_URI = "gitsm://github.com/orangepi-xunlong/firmware.git;protocol=https;branch=${SRCBRANCH}"

do_install () {
    install -d ${D}${base_libdir}/firmware
    install -m 0644 ${WORKDIR}/git/bt_configure_pskey.ini ${D}${base_libdir}/firmware/bt_configure_pskey.ini
    install -m 0644 ${WORKDIR}/git/bt_configure_rf.ini ${D}${base_libdir}/firmware/bt_configure_rf.ini
}

FILES:${PN} = "${base_libdir}/*"
