require conf/machine/include/sunxi64.inc

DEFAULTTUNE ?= "cortexa53-crypto"
require conf/machine/include/arm/armv8a/tune-cortexa53.inc

MACHINEOVERRIDES =. "sun50i:"

SOC_FAMILY = "sun50i-h616"
