require linux-mainline.inc

DESCRIPTION = "Mainline Longterm Linux kernel"

LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"

SRC_URI[sha256sum] = "e86917bba1990e967943645484182a64ba325f98b114a1906cc1d50992e073c1"

LINUX_VERSION = "6.1.31"

SRC_URI:append:orange-pi-zero3  = " \
        file://defconfig \
        file://0001-drv-wireless-add-uwe5622-wifi-driver.patch \
        file://0002-drv-wireless-driver-for-uwe5622-allwinner-bugfix.patch \
        file://0003-drv-fix-incldue-path-for-unisocwcn.patch \
        file://0004-drv-wireless-adapt-uwe5622-wifi-driver-to-kernel-6.1.patch \
        file://0005-drv-fix-setting-mac-address-for-netdev-in-uwe5622.patch \
        file://0006-drv-add-dump_reg-and-sunxi-sysinfo-drivers.patch \
        file://0007-drv-add-sunxi_get_soc_chipid-and-sunxi_get_serial.patch \
        file://0008-drv-add-sunxi-addr-driver.patch \
        file://0009-dts-add-addr_mgt-device-tree-node.patch \
        file://0011-dts-add-usb-to-h616.patch \
        file://1001-Orange-Pi-Zero3-support.patch \
        file://1002-Add-SUN8I_DE33_CCU-sun50i-h616-de33-mixer-allwinner-.patch \
        file://1003-allwinner-sun50i-h616-cpufreq-blocklist.patch \
        file://1004-nvmem-allwinner-sun50i-h616-sid-allwinner-sun50i-h61.patch \
        file://1005-allwinner-sun50i-h616-ths.patch \
        file://1006-Fixes-stack-trace-with-USB-init.patch \
        file://1007-Add-missing-spi-nor.patch \
        file://1008-allwinner-sun50i-h616-dma-required-for-sound.patch \
        file://1009-Add-sound-support.patch \
        file://1010-Set-the-broken-Park-link-status-quirk-specific-for-s.patch \
        file://1011-allwinner-sun50i-h616-video-engine.patch \
        file://1012-spi-dev.patch \
        file://1013-allwinner-sun50i-h616-lradc.patch \
        file://1014-sun6i_rtc_shutdown.patch \
        file://1015-drivers-net-phy.patch \
        file://1016-allwinner-sunxi-gmac.patch \
        file://1017-allwinner-sun50i-h616-cpufreq.patch \
        file://1018-drivers-opp-core.c-_opp_set_regulators.patch \
        file://1019-drivers-clk-sunxi-ng-Audio-PLL-SDM-settings.patch \
        file://1020-Enable-GPU.patch \
"
