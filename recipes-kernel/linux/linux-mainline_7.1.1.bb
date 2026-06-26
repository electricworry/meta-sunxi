require linux-mainline.inc

DESCRIPTION = "Mainline Longterm Linux kernel"

LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"

SRC_URI[sha256sum] = "5215fa3541dc7e7f5bcd51bf7e57f169cec6fce508ca54e3dc85fdee14371d7d"

LINUX_VERSION = "7.1.1"

SRC_URI:append:orange-pi-zero3  = " \
    file://defconfig \
    file://0001-clk-sunxi-ng-de2-Fix-Display-Engine-3.3-definitions.patch \
    file://0002-clk-sunxi-ng-de2-Export-register-regmap-for-DE33.patch \
    file://0003-drm-sun4i-Add-support-for-DE33-CSC.patch \
    file://0004-drm-sun4i-vi_layer-Limit-formats-for-DE33.patch \
    file://0005-dt-bindings-display-allwinner-Add-DE33-planes.patch \
    file://0006-drm-sun4i-Add-planes-driver.patch \
    file://0007-dt-bindings-display-allwinner-Split-H616-DE33-layer-.patch \
    file://0008-drm-sun4i-switch-DE33-to-new-bindings.patch \
    file://0009-srm-sun4i-Add-support-for-H616-HDMI-PHY.patch \
    file://0010-drm-sun4i-Add-compatible-for-H616-display-engine.patch \
    file://0011-arm64-dts-allwinner-h616-Add-display-pipeline.patch \
    file://0012-drm-sun4i-Add-H616-TCON-TV-support.patch \
    file://0013-arm64-dts-allwinner-h616-Enable-HDMI-on-several-boar.patch \
"

ERROR_QA:remove = "patch-status-core patch-status-noncore patch-status obsolete-license"
WARN_QA:remove = "patch-status-core patch-status-noncore patch-status obsolete-license"
