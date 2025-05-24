#!/bin/bash

# Define repositories and their target directories
declare -A REPOS=(
    ["device/xiaomi/sm8250-common"]="https://github.com/Matrixx-Devices/android_device_xiaomi_sm8250-common"
    ["device/xiaomi/camera"]="https://github.com/CuriousNom/device_xiaomi_camera.git"
    ["vendor/xiaomi/camera"]="https://gitlab.com/CuriousNom/vendor_xiaomi_camera.git"
    ["vendor/xiaomi/pipa"]="https://github.com/Matrixx-Devices/proprietary_vendor_xiaomi_pipa"
    ["vendor/xiaomi/sm8250-common"]="https://github.com/Matrixx-Devices/proprietary_vendor_xiaomi_sm8250-common"
    ["kernel/xiaomi/sm8250"]="https://github.com/Matrixx-Devices/android_kernel_xiaomi_pipa"
    ["hardware/dolby"]="https://github.com/Matrixx-Devices/hardware_dolby.git"
)

# Continue with other repos
for DIR in "${!REPOS[@]}"; do
    IFS='|' read -r REPO BRANCH <<< "${REPOS[$DIR]}"
    if [ -d "$DIR" ] && [ "$(ls -A "$DIR")" ]; then
        echo "[INFO] Skipping $DIR - already exists."
    else
        echo "[INFO] Cloning $REPO into $DIR..."
        if [ -n "$BRANCH" ]; then
            git clone --depth 1 -b "$BRANCH" "$REPO" "$DIR" || { echo "[ERROR] Failed to clone $REPO (branch: $BRANCH)"; exit 1; }
        else
            git clone --depth 1 "$REPO" "$DIR" || { echo "[ERROR] Failed to clone $REPO"; exit 1; }
        fi
    fi
done

# Hardware/xiaomi
HW_XIAOMI_DIR="hardware/xiaomi"
LOS_REPO="https://github.com/Matrixx-Devices/hardware_xiaomi.git"

if [ -d "$HW_XIAOMI_DIR" ]; then
    # Check if it's the los repo
    if git -C "$HW_XIAOMI_DIR" remote get-url origin 2>/dev/null | grep -q "$LOS_REPO"; then
        echo "[INFO] hardware/xiaomi is already tracking los, skipping..."
    else
        echo "[INFO] hardware/xiaomi is tracking a different repo. Replacing it with los..."
        rm -rf "$HW_XIAOMI_DIR"
        git clone --depth 1 "$LOS_REPO" "$HW_XIAOMI_DIR" || { echo "[ERROR] Failed to clone los hardware/xiaomi"; exit 1; }
    fi
else
    echo "[INFO] Cloning los hardware/xiaomi..."
    git clone --depth 1 "$LOS_REPO" "$HW_XIAOMI_DIR" || { echo "[ERROR] Failed to clone los hardware/xiaomi"; exit 1; }
fi

# Device Settings
DEVICESETTINGS_DIR="packages/resources/devicesettings"
DEVICESETTINGS_REPO="https://github.com/Matrixx-Devices/android_packages_resources_devicesettings.git"

if [ -d "$DEVICESETTINGS_DIR" ]; then
    # Check if it's the correct repo
    if git -C "$DEVICESETTINGS_DIR" remote get-url origin 2>/dev/null | grep -q "$DEVICESETTINGS_REPO"; then
        echo "[INFO] packages/resources/devicesettings is already tracking the correct repo, skipping..."
    else
        echo "[INFO] packages/resources/devicesettings is tracking a different repo. Replacing it..."
        rm -rf "$DEVICESETTINGS_DIR"
        git clone --depth 1 "$DEVICESETTINGS_REPO" "$DEVICESETTINGS_DIR" || { echo "[ERROR] Failed to clone devicesettings repo"; exit 1; }
    fi
else
    echo "[INFO] Cloning devicesettings repo..."
    git clone --depth 1 "$DEVICESETTINGS_REPO" "$DEVICESETTINGS_DIR" || { echo "[ERROR] Failed to clone devicesettings repo"; exit 1; }
fi

echo "[INFO] All repositories are set up!"

# AOSP recovery screen fix
ORIG_DIR=$(pwd)

# Navigate to bootable/recovery
cd bootable/recovery || exit

# Get the commit message of the commit we want to cherry-pick
COMMIT_MSG=$(git log -1 --format=%s 2e3bf15b0a249be01da27f7ceb3fdbfe0f5e9a82)

# Check if a commit with the same message is already in history
if git log --format=%s | grep -Fxq "$COMMIT_MSG"; then
    echo "Recovery fix commit already applied, skipping cherry-pick..."
else
    echo "Fetching and applying recovery fix commit..."
    git fetch https://github.com/CuriousNom/android_bootable_recovery.git
    if git cherry-pick 2e3bf15b0a249be01da27f7ceb3fdbfe0f5e9a82; then
        echo "Cherry-pick successful!"
    else
        echo "Cherry-pick failed! Aborting..."
        git cherry-pick --abort
    fi
fi

# Return to the original directory
cd "$ORIG_DIR"
