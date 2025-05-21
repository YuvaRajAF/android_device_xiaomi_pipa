#
# Copyright (C) 2021 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from sm8250-common
include device/xiaomi/sm8250-common/BoardConfigCommon.mk

# Inherit from proprietary files for miuicamera
-include vendor/xiaomi/pipa-miuicamera/products/board.mk

DEVICE_PATH := device/xiaomi/pipa

# Board
TARGET_BOARD_INFO_FILE := $(DEVICE_PATH)/board-info.txt

# Display
TARGET_SCREEN_DENSITY := 400

# Kernel
# Prebuilt Kernel
$(shell mkdir -p $(OUT_DIR)/target/product/pipa/obj/KERNEL_OBJ/usr)
KERNEL_LD := LD=ld.lld
TARGET_KERNEL_ADDITIONAL_FLAGS := DTC_EXT=$(shell pwd)/prebuilts/misc/linux-x86/dtc/dtc LLVM=1
TARGET_NO_KERNEL_OVERRIDE := true
TARGET_NO_KERNEL := false
BOARD_PREBUILT_DTBOIMAGE := $(DEVICE_PATH)-kernel/dtbo.img
TARGET_PREBUILT_KERNEL := $(DEVICE_PATH)-kernel/kernel
TARGET_PREBUILT_DTB := $(DEVICE_PATH)-kernel/dtb.img
TARGET_KERNEL_SOURCE := kernel/xiaomi/sm8250
PRODUCT_COPY_FILES += \
     $(DEVICE_PATH)-kernel/dtb.img:$(TARGET_COPY_OUT)/dtb.img \
     $(DEVICE_PATH)-kernel/kernel:$(TARGET_COPY_OUT)/kernel

# Properties
TARGET_SYSTEM_PROP += $(DEVICE_PATH)/system.prop
TARGET_VENDOR_PROP += $(DEVICE_PATH)/vendor.prop

# Sepolicy
BOARD_VENDOR_SEPOLICY_DIRS += $(DEVICE_PATH)/sepolicy/vendor

# Wi-Fi
SOONG_CONFIG_XIAOMI_KONA_WIFI_SYMLINK_VERSION := v2

# Inherit from the proprietary version
include vendor/xiaomi/pipa/BoardConfigVendor.mk
