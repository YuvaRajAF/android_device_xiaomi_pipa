#!/bin/bash

# Define repositories and their target directories
declare -A REPOS=(
    ["device/xiaomi/sm8250-common"]="https://github.com/Matrixx-Devices/android_device_xiaomi_sm8250-common"
    ["vendor/xiaomi/pipa"]="https://github.com/Matrixx-Devices/proprietary_vendor_xiaomi_pipa"
    ["vendor/xiaomi/sm8250-common"]="https://github.com/Matrixx-Devices/proprietary_vendor_xiaomi_sm8250-common"
    ["kernel/xiaomi/sm8250"]="https://github.com/Matrixx-Devices/android_kernel_xiaomi_pipa"
)

# Continue with other repos
for DIR in "${!REPOS[@]}"; do
    if [ -d "$DIR" ] && [ "$(ls -A "$DIR")" ]; then
        echo "[INFO] Skipping $DIR - already exists."
    else
        echo "[INFO] Cloning ${REPOS[$DIR]} into $DIR..."
        git clone --depth 1 "${REPOS[$DIR]}" "$DIR" || { echo "[ERROR] Failed to clone ${REPOS[$DIR]}"; exit 1; }
    fi
done

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
