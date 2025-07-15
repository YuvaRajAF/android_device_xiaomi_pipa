#
# Copyright (C) 2021 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base.mk)

# Inherit some common lineage stuff.
$(call inherit-product, vendor/lineage/config/common_full_tablet_wifionly.mk)

# Inherit keys
$(call inherit-product, vendor/lineage-priv/keys/keys.mk)

# Axion specific
PRODUCT_NO_CAMERA := false
AXION_CAMERA_REAR_INFO := 13
AXION_CAMERA_FRONT_INFO := 8
AXION_CPU_SMALL_CORES := 0,1,2,3
AXION_CPU_BIG_CORES := 4,5,6,7
AXION_MAINTAINER := Yuvaraj_(Atom_YR)
AXION_PROCESSOR := Qualcomm_Snapdragon_870
TARGET_PREBUILT_BCR := false
TARGET_DISABLE_EPPE := true
TARGET_SUPPORTS_WALLEFFECT := true
BYPASS_CHARGE_SUPPORTED  := true

# Inherit from pipa device
$(call inherit-product, device/xiaomi/pipa/device.mk)

# Inherit Camera-related flags
TARGET_USES_MIUI_CAMERA := true
TARGET_INCLUDES_MIUI_CAMERA := true

PRODUCT_NAME := lineage_pipa
PRODUCT_DEVICE := pipa
PRODUCT_MANUFACTURER := Xiaomi
PRODUCT_BRAND := Xiaomi
PRODUCT_MODEL := Pad 6

PRODUCT_CHARACTERISTICS := tablet
TARGET_SUPPORTS_QUICK_TAP := false

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildFingerprint=Xiaomi/pipa_global/pipa:13/RKQ1.211001.001/V816.0.7.0.UMZMIXM:user/release-keys
