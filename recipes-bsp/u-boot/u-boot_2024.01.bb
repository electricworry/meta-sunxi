HOMEPAGE = "http://www.denx.de/wiki/U-Boot/WebHome"
DESCRIPTION = "U-Boot, a boot loader for Embedded boards based on PowerPC, \
ARM, MIPS and several other processors, which can be installed in a boot \
ROM and used to initialize and test the hardware or to download and run \
application code."
SECTION = "bootloaders"
DEPENDS += "flex-native bison-native python3-setuptools-native"

LICENSE = "GPL-2.0-or-later"
LIC_FILES_CHKSUM = "file://Licenses/README;md5=2ca5f2c35c8cc335f0a19756634782f1"
PE = "1"

# We use the revision in order to avoid having to fetch it from the
# repo during parse
SRCREV = "866ca972d6c3cabeaf6dbac431e8e08bb30b3c8e"

SRC_URI = "git://source.denx.de/u-boot/u-boot.git;protocol=https;branch=master \
           file://CVE-2024-57254.patch \
           file://CVE-2024-57255.patch \
           file://CVE-2024-57256.patch \
           file://CVE-2024-57257.patch \
           file://CVE-2024-57258-1.patch \
           file://CVE-2024-57258-2.patch \
           file://CVE-2024-57258-3.patch \
           file://CVE-2024-57259.patch \
           file://CVE-2024-42040.patch \
"

#S = "${WORKDIR}/git"
B = "${WORKDIR}/build"

inherit pkgconfig

do_configure[cleandirs] = "${B}"

SUMMARY = "Universal Boot Loader for embedded devices"
PROVIDES = "virtual/bootloader"

PACKAGE_ARCH = "${MACHINE_ARCH}"

DEPENDS += "${@bb.utils.contains('UBOOT_ENV_SUFFIX', 'scr', 'u-boot-mkimage-native', '', d)}"

inherit uboot-config uboot-extlinux-config uboot-sign deploy python3native kernel-arch

DEPENDS += "swig-native"

EXTRA_OEMAKE = 'CROSS_COMPILE=${TARGET_PREFIX} CC="${TARGET_PREFIX}gcc ${TOOLCHAIN_OPTIONS} ${DEBUG_PREFIX_MAP}" V=1'
EXTRA_OEMAKE += 'HOSTCC="${BUILD_CC} ${BUILD_CFLAGS} ${BUILD_LDFLAGS}"'
EXTRA_OEMAKE += 'STAGING_INCDIR=${STAGING_INCDIR_NATIVE} STAGING_LIBDIR=${STAGING_LIBDIR_NATIVE}'

PACKAGECONFIG ??= "openssl"
# u-boot will compile its own tools during the build, with specific
# configurations (aka when CONFIG_FIT_SIGNATURE is enabled) openssl is needed as
# a host build dependency.
PACKAGECONFIG[openssl] = ",,openssl-native"

# Allow setting an additional version string that will be picked up by the
# u-boot build system and appended to the u-boot version.  If the .scmversion
# file already exists it will not be overwritten.
UBOOT_LOCALVERSION ?= ""

# Default name of u-boot initial env, but enable individual recipes to change
# this value.
UBOOT_INITIAL_ENV ?= "${PN}-initial-env"

# This provides the logic for creating the desired u-boot config,
# accounting for any *.cfg files added to SRC_URI.  It's separated
# from u-boot.inc for use by recipes that need u-boot properly
# configured but aren't doing a full build of u-boot itself (such as
# its companion tools).

inherit uboot-config cml1

DEPENDS += "kern-tools-native"

CONFIGURE_FILES = "${@d.getVar('UBOOT_MACHINE') or '.config'}"

do_configure () {
    if [ -n "${UBOOT_CONFIG}" ]; then
        unset i j
        for config in ${UBOOT_MACHINE}; do
            i=$(expr $i + 1);
            for type in ${UBOOT_CONFIG}; do
                j=$(expr $j + 1);
                if [ $j -eq $i ]; then
                    uboot_configure_config $config $type
                fi
            done
            unset j
        done
        unset i
    else
        uboot_configure
    fi
}

uboot_configure_config () {
    config=$1
    type=$2

    oe_runmake -C ${S} O=${B}/${config} ${config}
    if [ -n "${@' '.join(find_cfgs(d))}" ]; then
        merge_config.sh -m -O ${B}/${config} ${B}/${config}/.config ${@" ".join(find_cfgs(d))}
        oe_runmake -C ${S} O=${B}/${config} oldconfig
    fi
}

uboot_configure () {
    if [ -n "${UBOOT_MACHINE}" ]; then
        oe_runmake -C ${S} O=${B} ${UBOOT_MACHINE}
    else
        oe_runmake -C ${S} O=${B} oldconfig
    fi
    merge_config.sh -m .config ${@" ".join(find_cfgs(d))}
    cml1_do_configure
}



do_savedefconfig() {
	bbplain "Saving defconfig to:\n${B}/defconfig"
	oe_runmake -C ${B} savedefconfig
}
do_savedefconfig[nostamp] = "1"
addtask savedefconfig after do_configure
UBOOT_ARCH_DIR = "${@'arm' if d.getVar('UBOOT_ARCH').startswith('arm') else d.getVar('UBOOT_ARCH')}"
do_compile () {
    if [ "${@bb.utils.filter('DISTRO_FEATURES', 'ld-is-gold', d)}" ]; then
        sed -i 's/$(CROSS_COMPILE)ld$/$(CROSS_COMPILE)ld.bfd/g' ${S}/config.mk
    fi

    unset LDFLAGS
    unset CFLAGS
    unset CPPFLAGS

    if [ ! -e ${B}/.scmversion -a ! -e ${S}/.scmversion ]
    then
        echo ${UBOOT_LOCALVERSION} > ${B}/.scmversion
        echo ${UBOOT_LOCALVERSION} > ${S}/.scmversion
    fi

    if [ -n "${UBOOT_CONFIG}" -o -n "${UBOOT_DELTA_CONFIG}" ]
    then
        unset i j
        for config in ${UBOOT_MACHINE}; do
            i=$(expr $i + 1);
            for type in ${UBOOT_CONFIG}; do
                j=$(expr $j + 1);
                if [ $j -eq $i ]
                then
                    uboot_compile_config $i $config $type
                fi
            done
            unset j
        done
        unset i
    else
        uboot_compile
    fi

    if [ -n "${UBOOT_ENV}" ] && [ "${UBOOT_ENV_SUFFIX}" = "scr" ]
    then
        ${UBOOT_MKIMAGE} -C none -A ${UBOOT_ARCH} -T script -d ${WORKDIR}/sources/${UBOOT_ENV_SRC} ${WORKDIR}/${UBOOT_ENV_BINARY}
    fi
}

uboot_compile_config () {
    i=$1
    config=$2
    type=$3

    oe_runmake -C ${S} O=${B}/${config} ${UBOOT_MAKE_TARGET}

    unset k
    for binary in ${UBOOT_BINARIES}; do
        k=$(expr $k + 1);
        if [ $k -eq $i ]; then
            uboot_compile_config_copy_binary $config $type $binary
        fi
    done
    unset k

    # Generate the uboot-initial-env
    if [ -n "${UBOOT_INITIAL_ENV}" ]; then
        oe_runmake -C ${S} O=${B}/${config} u-boot-initial-env
        cp ${B}/${config}/u-boot-initial-env ${B}/${config}/u-boot-initial-env-${type}
    fi
}

uboot_compile_config_copy_binary () {
    config=$1
    type=$2
    binary=$3

    cp ${B}/${config}/${binary} ${B}/${config}/${UBOOT_BINARYNAME}-${type}.${UBOOT_SUFFIX}
}

uboot_compile () {
    oe_runmake -C ${S} O=${B} ${UBOOT_MAKE_TARGET}

    # Generate the uboot-initial-env
    if [ -n "${UBOOT_INITIAL_ENV}" ]; then
        oe_runmake -C ${S} O=${B} u-boot-initial-env
    fi
}

do_install () {
    if [ -n "${UBOOT_CONFIG}" ]
    then
        for config in ${UBOOT_MACHINE}; do
            i=$(expr $i + 1);
            for type in ${UBOOT_CONFIG}; do
                j=$(expr $j + 1);
                if [ $j -eq $i ]
                then
                    uboot_install_config $config $type
                fi
            done
            unset j
        done
        unset i
    else
        uboot_install
    fi

    if [ -n "${UBOOT_ELF}" ]
    then
        if [ -n "${UBOOT_CONFIG}" ]
        then
            for config in ${UBOOT_MACHINE}; do
                i=$(expr $i + 1);
                for type in ${UBOOT_CONFIG}; do
                    j=$(expr $j + 1);
                    if [ $j -eq $i ]
                    then
                        uboot_install_elf_config $config $type
                    fi
                done
                unset j
            done
            unset i
        else
            uboot_install_elf
        fi
    fi

    if [ -e ${WORKDIR}/fw_env.config ] ; then
        install -d ${D}${sysconfdir}
        install -m 644 ${WORKDIR}/fw_env.config ${D}${sysconfdir}/fw_env.config
    fi

    if [ -n "${SPL_BINARY}" ]
    then
        if [ -n "${UBOOT_CONFIG}" ]
        then
            for config in ${UBOOT_MACHINE}; do
                i=$(expr $i + 1);
                for type in ${UBOOT_CONFIG}; do
                    j=$(expr $j + 1);
                    if [ $j -eq $i ]
                    then
                        uboot_install_spl_config $config $type
                    fi
                done
                unset j
            done
            unset i
        else
            uboot_install_spl
        fi
    fi

    if [ -n "${UBOOT_ENV}" ]
    then
        install -m 644 ${WORKDIR}/${UBOOT_ENV_BINARY} ${D}/boot/${UBOOT_ENV_IMAGE}
        ln -sf ${UBOOT_ENV_IMAGE} ${D}/boot/${UBOOT_ENV_BINARY}
    fi

    if [ "${UBOOT_EXTLINUX}" = "1" ]
    then
        install -Dm 0644 ${UBOOT_EXTLINUX_CONFIG} ${D}/${UBOOT_EXTLINUX_INSTALL_DIR}/${UBOOT_EXTLINUX_CONF_NAME}
    fi
}

uboot_install_config () {
    config=$1
    type=$2

    install -D -m 644 ${B}/${config}/${UBOOT_BINARYNAME}-${type}.${UBOOT_SUFFIX} ${D}/boot/${UBOOT_BINARYNAME}-${type}-${UBOOT_VERSION}.${UBOOT_SUFFIX}
    ln -sf ${UBOOT_BINARYNAME}-${type}-${UBOOT_VERSION}.${UBOOT_SUFFIX} ${D}/boot/${UBOOT_BINARY}-${type}
    ln -sf ${UBOOT_BINARYNAME}-${type}-${UBOOT_VERSION}.${UBOOT_SUFFIX} ${D}/boot/${UBOOT_BINARY}

    # Install the uboot-initial-env
    if [ -n "${UBOOT_INITIAL_ENV}" ]; then
        install -D -m 644 ${B}/${config}/u-boot-initial-env-${type} ${D}/${sysconfdir}/${UBOOT_INITIAL_ENV}-${MACHINE}-${type}-${UBOOT_VERSION}
        ln -sf ${UBOOT_INITIAL_ENV}-${MACHINE}-${type}-${UBOOT_VERSION} ${D}/${sysconfdir}/${UBOOT_INITIAL_ENV}-${MACHINE}-${type}
        ln -sf ${UBOOT_INITIAL_ENV}-${MACHINE}-${type}-${UBOOT_VERSION} ${D}/${sysconfdir}/${UBOOT_INITIAL_ENV}-${type}
        ln -sf ${UBOOT_INITIAL_ENV}-${MACHINE}-${type}-${UBOOT_VERSION} ${D}/${sysconfdir}/${UBOOT_INITIAL_ENV}
    fi
}

uboot_install () {
    install -D -m 644 ${B}/${UBOOT_BINARY} ${D}/boot/${UBOOT_IMAGE}
    ln -sf ${UBOOT_IMAGE} ${D}/boot/${UBOOT_BINARY}

    # Install the uboot-initial-env
    if [ -n "${UBOOT_INITIAL_ENV}" ]; then
        install -D -m 644 ${B}/u-boot-initial-env ${D}/${sysconfdir}/${UBOOT_INITIAL_ENV}-${MACHINE}-${UBOOT_VERSION}
        ln -sf ${UBOOT_INITIAL_ENV}-${MACHINE}-${UBOOT_VERSION} ${D}/${sysconfdir}/${UBOOT_INITIAL_ENV}-${MACHINE}
        ln -sf ${UBOOT_INITIAL_ENV}-${MACHINE}-${UBOOT_VERSION} ${D}/${sysconfdir}/${UBOOT_INITIAL_ENV}
    fi
}

uboot_install_elf_config () {
    config=$1
    type=$2

    install -m 644 ${B}/${config}/${UBOOT_ELF} ${D}/boot/u-boot-${type}-${UBOOT_VERSION}.${UBOOT_ELF_SUFFIX}
    ln -sf u-boot-${type}-${UBOOT_VERSION}.${UBOOT_ELF_SUFFIX} ${D}/boot/${UBOOT_BINARY}-${type}
    ln -sf u-boot-${type}-${UBOOT_VERSION}.${UBOOT_ELF_SUFFIX} ${D}/boot/${UBOOT_BINARY}
}

uboot_install_elf () {
    install -m 644 ${B}/${UBOOT_ELF} ${D}/boot/${UBOOT_ELF_IMAGE}
    ln -sf ${UBOOT_ELF_IMAGE} ${D}/boot/${UBOOT_ELF_BINARY}
}

uboot_install_spl_config () {
    config=$1
    type=$2

    install -m 644 ${B}/${config}/${SPL_BINARY} ${D}/boot/${SPL_BINARYNAME}-${type}-${UBOOT_VERSION}${SPL_DELIMITER}${SPL_SUFFIX}
    ln -sf ${SPL_BINARYNAME}-${type}-${UBOOT_VERSION}${SPL_DELIMITER}${SPL_SUFFIX} ${D}/boot/${SPL_BINARYFILE}-${type}
    ln -sf ${SPL_BINARYNAME}-${type}-${UBOOT_VERSION}${SPL_DELIMITER}${SPL_SUFFIX} ${D}/boot/${SPL_BINARYFILE}
}

uboot_install_spl () {
    install -m 644 ${B}/${SPL_BINARY} ${D}/boot/${SPL_IMAGE}
    ln -sf ${SPL_IMAGE} ${D}/boot/${SPL_BINARYFILE}
}

PACKAGE_BEFORE_PN += "${PN}-env ${PN}-extlinux"

RPROVIDES:${PN}-env += "u-boot-default-env"
ALLOW_EMPTY:${PN}-env = "1"
FILES:${PN}-env = " \
    ${@ '${sysconfdir}/${UBOOT_INITIAL_ENV}*' if d.getVar('UBOOT_INITIAL_ENV') else ''} \
    ${sysconfdir}/fw_env.config \
"

FILES:${PN}-extlinux = "${UBOOT_EXTLINUX_INSTALL_DIR}/${UBOOT_EXTLINUX_CONF_NAME}"
RDEPENDS:${PN} += "${@bb.utils.contains('UBOOT_EXTLINUX', '1', '${PN}-extlinux', '', d)}"

SYSROOT_DIRS += "/boot"
FILES:${PN} = "/boot ${datadir}"
RDEPENDS:${PN} += "${PN}-env"

do_deploy () {
    if [ -n "${UBOOT_CONFIG}" ]
    then
        for config in ${UBOOT_MACHINE}; do
            i=$(expr $i + 1);
            for type in ${UBOOT_CONFIG}; do
                j=$(expr $j + 1);
                if [ $j -eq $i ]
                then
                    uboot_deploy_config $config $type
                fi
            done
            unset j
        done
        unset i
    else
        uboot_deploy
    fi

    if [ -e ${WORKDIR}/fw_env.config ] ; then
        install -D -m 644 ${WORKDIR}/fw_env.config ${DEPLOYDIR}/fw_env.config-${MACHINE}-${UBOOT_VERSION}
        cd ${DEPLOYDIR}
        ln -sf fw_env.config-${MACHINE}-${UBOOT_VERSION} fw_env.config-${MACHINE}
        ln -sf fw_env.config-${MACHINE}-${UBOOT_VERSION} fw_env.config
    fi

    if [ -n "${UBOOT_ELF}" ]
    then
        if [ -n "${UBOOT_CONFIG}" ]
        then
            for config in ${UBOOT_MACHINE}; do
                i=$(expr $i + 1);
                for type in ${UBOOT_CONFIG}; do
                    j=$(expr $j + 1);
                    if [ $j -eq $i ]
                    then
                        uboot_deploy_elf_config $config $type
                    fi
                done
                unset j
            done
            unset i
        else
            uboot_deploy_elf
        fi
    fi


     if [ -n "${SPL_BINARY}" ]
     then
        if [ -n "${UBOOT_CONFIG}" ]
        then
            for config in ${UBOOT_MACHINE}; do
                i=$(expr $i + 1);
                for type in ${UBOOT_CONFIG}; do
                    j=$(expr $j + 1);
                    if [ $j -eq $i ]
                    then
                        uboot_deploy_spl_config $config $type
                    fi
                done
                unset j
            done
            unset i
        else
            uboot_deploy_spl
        fi
    fi

    if [ -n "${UBOOT_ENV}" ]
    then
        install -m 644 ${WORKDIR}/${UBOOT_ENV_BINARY} ${DEPLOYDIR}/${UBOOT_ENV_IMAGE}
        ln -sf ${UBOOT_ENV_IMAGE} ${DEPLOYDIR}/${UBOOT_ENV_BINARY}
        ln -sf ${UBOOT_ENV_IMAGE} ${DEPLOYDIR}/${UBOOT_ENV_SYMLINK}
    fi

    if [ "${UBOOT_EXTLINUX}" = "1" ]
    then
        install -m 644 ${UBOOT_EXTLINUX_CONFIG} ${DEPLOYDIR}/${UBOOT_EXTLINUX_SYMLINK}
        ln -sf ${UBOOT_EXTLINUX_SYMLINK} ${DEPLOYDIR}/${UBOOT_EXTLINUX_CONF_NAME}-${MACHINE}
        ln -sf ${UBOOT_EXTLINUX_SYMLINK} ${DEPLOYDIR}/${UBOOT_EXTLINUX_CONF_NAME}
    fi

    if [ -n "${UBOOT_DTB}" ]
    then
        install -m 644 ${B}/arch/${UBOOT_ARCH_DIR}/dts/${UBOOT_DTB_BINARY} ${DEPLOYDIR}/
    fi
}

uboot_deploy_config () {
    config=$1
    type=$2

    install -D -m 644 ${B}/${config}/${UBOOT_BINARYNAME}-${type}.${UBOOT_SUFFIX} ${DEPLOYDIR}/${UBOOT_BINARYNAME}-${type}-${UBOOT_VERSION}.${UBOOT_SUFFIX}
    cd ${DEPLOYDIR}
    ln -sf ${UBOOT_BINARYNAME}-${type}-${UBOOT_VERSION}.${UBOOT_SUFFIX} ${UBOOT_SYMLINK}-${type}
    ln -sf ${UBOOT_BINARYNAME}-${type}-${UBOOT_VERSION}.${UBOOT_SUFFIX} ${UBOOT_SYMLINK}
    ln -sf ${UBOOT_BINARYNAME}-${type}-${UBOOT_VERSION}.${UBOOT_SUFFIX} ${UBOOT_BINARY}-${type}
    ln -sf ${UBOOT_BINARYNAME}-${type}-${UBOOT_VERSION}.${UBOOT_SUFFIX} ${UBOOT_BINARY}

    # Deploy the uboot-initial-env
    if [ -n "${UBOOT_INITIAL_ENV}" ]; then
        install -D -m 644 ${B}/${config}/u-boot-initial-env-${type} ${DEPLOYDIR}/${UBOOT_INITIAL_ENV}-${MACHINE}-${type}-${UBOOT_VERSION}
        cd ${DEPLOYDIR}
        ln -sf ${UBOOT_INITIAL_ENV}-${MACHINE}-${type}-${UBOOT_VERSION} ${UBOOT_INITIAL_ENV}-${MACHINE}-${type}
        ln -sf ${UBOOT_INITIAL_ENV}-${MACHINE}-${type}-${UBOOT_VERSION} ${UBOOT_INITIAL_ENV}-${type}
    fi
}

uboot_deploy () {
    install -D -m 644 ${B}/${UBOOT_BINARY} ${DEPLOYDIR}/${UBOOT_IMAGE}

    cd ${DEPLOYDIR}
    rm -f ${UBOOT_BINARY} ${UBOOT_SYMLINK}
    ln -sf ${UBOOT_IMAGE} ${UBOOT_SYMLINK}
    ln -sf ${UBOOT_IMAGE} ${UBOOT_BINARY}

    # Deploy the uboot-initial-env
    if [ -n "${UBOOT_INITIAL_ENV}" ]; then
        install -D -m 644 ${B}/u-boot-initial-env ${DEPLOYDIR}/${UBOOT_INITIAL_ENV}-${MACHINE}-${UBOOT_VERSION}
        cd ${DEPLOYDIR}
        ln -sf ${UBOOT_INITIAL_ENV}-${MACHINE}-${UBOOT_VERSION} ${UBOOT_INITIAL_ENV}-${MACHINE}
        ln -sf ${UBOOT_INITIAL_ENV}-${MACHINE}-${UBOOT_VERSION} ${UBOOT_INITIAL_ENV}
    fi
}

uboot_deploy_elf_config () {
    config=$1
    type=$2

    install -m 644 ${B}/${config}/${UBOOT_ELF} ${DEPLOYDIR}/u-boot-${type}-${UBOOT_VERSION}.${UBOOT_ELF_SUFFIX}
    ln -sf u-boot-${type}-${UBOOT_VERSION}.${UBOOT_ELF_SUFFIX} ${DEPLOYDIR}/${UBOOT_ELF_BINARY}-${type}
    ln -sf u-boot-${type}-${UBOOT_VERSION}.${UBOOT_ELF_SUFFIX} ${DEPLOYDIR}/${UBOOT_ELF_BINARY}
    ln -sf u-boot-${type}-${UBOOT_VERSION}.${UBOOT_ELF_SUFFIX} ${DEPLOYDIR}/${UBOOT_ELF_SYMLINK}-${type}
    ln -sf u-boot-${type}-${UBOOT_VERSION}.${UBOOT_ELF_SUFFIX} ${DEPLOYDIR}/${UBOOT_ELF_SYMLINK}
}

uboot_deploy_elf () {
    install -m 644 ${B}/${UBOOT_ELF} ${DEPLOYDIR}/${UBOOT_ELF_IMAGE}
    ln -sf ${UBOOT_ELF_IMAGE} ${DEPLOYDIR}/${UBOOT_ELF_BINARY}
    ln -sf ${UBOOT_ELF_IMAGE} ${DEPLOYDIR}/${UBOOT_ELF_SYMLINK}
}

uboot_deploy_spl_config () {
    config=$1
    type=$2

    install -m 644 ${B}/${config}/${SPL_BINARY} ${DEPLOYDIR}/${SPL_BINARYNAME}-${type}-${UBOOT_VERSION}${SPL_DELIMITER}${SPL_SUFFIX}
    rm -f ${DEPLOYDIR}/${SPL_BINARYFILE} ${DEPLOYDIR}/${SPL_SYMLINK}
    ln -sf ${SPL_BINARYNAME}-${type}-${UBOOT_VERSION}${SPL_DELIMITER}${SPL_SUFFIX} ${DEPLOYDIR}/${SPL_BINARYFILE}-${type}
    ln -sf ${SPL_BINARYNAME}-${type}-${UBOOT_VERSION}${SPL_DELIMITER}${SPL_SUFFIX} ${DEPLOYDIR}/${SPL_BINARYFILE}
    ln -sf ${SPL_BINARYNAME}-${type}-${UBOOT_VERSION}${SPL_DELIMITER}${SPL_SUFFIX} ${DEPLOYDIR}/${SPL_SYMLINK}-${type}
    ln -sf ${SPL_BINARYNAME}-${type}-${UBOOT_VERSION}${SPL_DELIMITER}${SPL_SUFFIX} ${DEPLOYDIR}/${SPL_SYMLINK}
}

uboot_deploy_spl () {
    install -m 644 ${B}/${SPL_BINARY} ${DEPLOYDIR}/${SPL_IMAGE}
    ln -sf ${SPL_IMAGE} ${DEPLOYDIR}/${SPL_BINARYNAME}
    ln -sf ${SPL_IMAGE} ${DEPLOYDIR}/${SPL_SYMLINK}
}

addtask deploy before do_build after do_compile


DEPENDS += "bc-native dtc-native python3-pyelftools-native"


ERROR_QA:remove = "patch-status-core patch-status-noncore patch-status obsolete-license"
WARN_QA:remove = "patch-status-core patch-status-noncore patch-status obsolete-license"
