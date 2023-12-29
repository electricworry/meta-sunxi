DESCRIPTION = "Enable Orange Pi Bluetooth"
SECTION = "network"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://${WORKDIR}/git/LICENSE;md5=e8c1458438ead3c34974bc0be3a03ed6"

RDEPENDS:${PN} += "bash rfkill orangepi-firmware"

SRCBRANCH = "next"
SRCREV  = "a8686ef79c020c4070068b020e86e0d87a309409"

SRC_URI = "gitsm://github.com/orangepi-xunlong/orangepi-build.git;protocol=https;branch=${SRCBRANCH} \
		   file://hciattach_opi.init \
           file://hciattach_opi.sh \
           file://hciattach_opi.service"

inherit update-rc.d systemd

INITSCRIPT_NAME = "hciattach_opi"
INITSCRIPT_PARAMS = "start 2 5 ."
SYSTEMD_SERVICE:${PN} = "hciattach_opi.service"

do_install () {
	install -d ${D}${sbindir}
    install -m 700 \
		${WORKDIR}/git/external/config/optional/families/sun50iw9/_packages/bsp-cli/usr/bin/${INITSCRIPT_NAME} \
		${D}${sbindir}/${INITSCRIPT_NAME}
    install -m 700 ${WORKDIR}/${INITSCRIPT_NAME}.sh ${D}${sbindir}/${INITSCRIPT_NAME}.sh

	if ${@bb.utils.contains('DISTRO_FEATURES', 'sysvinit', 'true', 'false', d)}; then
		install -d ${D}${sysconfdir}/init.d/
		install -m 755 ${WORKDIR}/${INITSCRIPT_NAME}.init ${D}${sysconfdir}/init.d/${INITSCRIPT_NAME}
		sed -i -e 's,@SBINDIR@,${sbindir},g' \
			${D}${sysconfdir}/init.d/*
	fi

	if ${@bb.utils.contains('DISTRO_FEATURES', 'systemd', 'true', 'false', d)}; then
		install -d ${D}${systemd_system_unitdir}
		install -m 644 ${WORKDIR}/${INITSCRIPT_NAME}.service ${D}${systemd_system_unitdir}/${INITSCRIPT_NAME}.service
		sed -i -e 's,@SBINDIR@,${sbindir},g' \
			${D}${systemd_system_unitdir}/*.service
	fi
}
