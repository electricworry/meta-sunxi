FILESEXTRAPATHS:prepend:orange-pi-zero3 := "${THISDIR}/orange-pi-zero3:"

SRC_URI:append:orange-pi-zero3 = " file://orange-pi-zero3.pa"

do_install:append:orange-pi-zero3() {
	install -d ${D}${sysconfdir}/pulse/default.pa.d
	install -m 0644 ${WORKDIR}/orange-pi-zero3.pa  ${D}${sysconfdir}/pulse/default.pa.d/orange-pi-zero3.pa
    sed -i "s/^auto-profiles .*/auto-profiles = no/" ${D}${datadir}/pulseaudio/alsa-mixer/profile-sets/default.conf
}
