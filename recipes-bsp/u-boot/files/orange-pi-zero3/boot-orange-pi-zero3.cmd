# Default to (primary) SD
rootdev=mmcblk0p2
# I'm pretty sure this check is not valid now.
# The egon_head is at SPL_ADDR (0x20000), and boot_media is 0x28 from there.
# Correct location for test is at 0x20000.
if itest.b *0x28 == 0x00 ; then
	# U-Boot loaded from eMMC or secondary SD so use it for rootfs too
	echo "U-boot loaded from eMMC or secondary SD"
	rootdev=mmcblk1p2
fi

# However, it doesn't matter for OrangePi Zero3, as we *did* boot from
# SUNXI_BOOTED_FROM_MMC0, but for some reason kernel has partition at
# mmcblk1p2.
# My workaround is to use the PARTUUID.
part uuid mmc 0:2 partuuid

setenv bootargs console=${console} console=tty1 root=PARTUUID=${partuuid} rootwait panic=10 ${extra}
load mmc 0:1 ${fdt_addr_r} ${fdtfile}
load mmc 0:1 ${kernel_addr_r} Image
booti ${kernel_addr_r} - ${fdt_addr_r}

