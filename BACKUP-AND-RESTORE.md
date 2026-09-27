# Personal Backup and Restore

This document describes how to create a local backup ZIP from APK files already obtained by the owner and how to restore the working setup later.

## Working APKs

Melolo:
- com.worldance.drama.apk
- config.armeabi_v7a.apk
- config.xxhdpi.apk

LTvLauncher:
- LTvLauncher-armeabi-v7a-release.apk

These APK binaries are intentionally not stored in this public repository.

## One-command local backup

Run on the Mac:

    cd ~/Desktop && rm -rf Melolo-Mecool-Backup && mkdir -p Melolo-Mecool-Backup && cp ~/Desktop/Melolo-4.9.2/com.worldance.drama.apk ~/Desktop/Melolo-Mecool-Backup/ && cp ~/Desktop/Melolo-4.9.2/config.armeabi_v7a.apk ~/Desktop/Melolo-Mecool-Backup/ && cp ~/Desktop/Melolo-4.9.2/config.xxhdpi.apk ~/Desktop/Melolo-Mecool-Backup/ && cp ~/Desktop/LTvLauncher-armeabi-v7a-release.apk ~/Desktop/Melolo-Mecool-Backup/ && printf '%s\n' '#!/bin/bash' '' 'ADB="/Users/anotai/Desktop/platform-tools/adb"' 'echo "=== Connect to Mecool ==="' 'read -p "Enter Mecool IP:PORT (example 192.168.1.117:39517): " DEVICE' 'echo "Checking connection..."' '"$ADB" connect "$DEVICE"' 'echo "=== Checking architecture ==="' '"$ADB" -s "$DEVICE" shell getprop ro.product.cpu.abilist' 'echo "=== Installing Melolo 4.9.2 ==="' '"$ADB" -s "$DEVICE" install-multiple com.worldance.drama.apk config.armeabi_v7a.apk config.xxhdpi.apk' 'echo "=== Installing LTvLauncher ==="' '"$ADB" -s "$DEVICE" install LTvLauncher-armeabi-v7a-release.apk' 'echo "=== Verifying ==="' '"$ADB" -s "$DEVICE" shell pm list packages | grep -E "worldance|leanbitlab"' 'echo "=== Launching LTvLauncher ==="' '"$ADB" -s "$DEVICE" shell monkey -p com.leanbitlab.ltvL 1' 'echo "Done. Use the Mecool remote to select Melolo in LTvLauncher."' > Melolo-Mecool-Backup/reinstall.sh && chmod +x Melolo-Mecool-Backup/reinstall.sh && zip -r Melolo-Mecool-Backup.zip Melolo-Mecool-Backup

## Restore

1. Unzip the personal backup ZIP to ~/Desktop.
2. Open Terminal in the extracted Melolo-Mecool-Backup folder.
3. Run:

    ./reinstall.sh

4. Enter the Mecool's current Wireless Debugging IP:PORT when prompted.

The port may change after restarting or re-enabling Wireless debugging.

## Important

Do not disable the existing Google TV launcher unless you intentionally want to replace it. LTvLauncher can coexist with Google TV.

## Architecture warning

This setup was tested on a Mecool Android TV 14 device reporting:

    armeabi-v7a,armeabi

Do not use the arm64_v8a Melolo split on this device; the earlier 5.4.8 package failed with INSTALL_FAILED_NO_MATCHING_ABIS.
