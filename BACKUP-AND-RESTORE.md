# Personal Backup and Restore

This document describes how to create a local backup ZIP from APK files already obtained by the
owner and how to restore the working setup later.

## Working files

Melolo:
- com.worldance.drama.apk
- config.armeabi_v7a.apk
- config.xxhdpi.apk

Melolo TV tile:
- melolo-tile-debug.apk (built from mecool-tv/, see mecool-tv/README.md)
- ~/.android/debug.keystore (the key the tile is signed with; updating an installed tile needs
  the same key, otherwise uninstall it first)

LTvLauncher (optional, no longer needed):
- LTvLauncher-armeabi-v7a-release.apk

These APK binaries and the keystore are intentionally not stored in this public repository.
Never commit debug.keystore.

## One-command local backup

Run on the Mac (adjust the path to your clone of this repository):

    REPO=~/melolo-in-mekool && cd ~/Desktop && rm -rf Melolo-Mecool-Backup && mkdir -p Melolo-Mecool-Backup && cp ~/Desktop/Melolo-4.9.2/com.worldance.drama.apk ~/Desktop/Melolo-4.9.2/config.armeabi_v7a.apk ~/Desktop/Melolo-4.9.2/config.xxhdpi.apk Melolo-Mecool-Backup/ && cp "$REPO/mecool-tv/melolo-tile/build/outputs/apk/debug/melolo-tile-debug.apk" Melolo-Mecool-Backup/ && cp ~/.android/debug.keystore Melolo-Mecool-Backup/ && printf '%s\n' '#!/bin/bash' '' 'cd "$(dirname "$0")"' 'ADB="$HOME/Desktop/platform-tools/adb"' 'echo "=== Connect to Mecool ==="' 'read -p "Enter Mecool IP:PORT (example 192.168.1.122:5555): " DEVICE' '"$ADB" connect "$DEVICE"' 'echo "=== Checking architecture ==="' '"$ADB" -s "$DEVICE" shell getprop ro.product.cpu.abilist' 'echo "=== Installing Melolo 4.9.2 ==="' '"$ADB" -s "$DEVICE" install-multiple com.worldance.drama.apk config.armeabi_v7a.apk config.xxhdpi.apk' 'echo "=== Enabling freeform windows ==="' '"$ADB" -s "$DEVICE" shell settings put global enable_freeform_support 1' '"$ADB" -s "$DEVICE" shell settings put global force_resizable_activities 1' 'echo "=== Installing Melolo TV tile ==="' '"$ADB" -s "$DEVICE" install -r melolo-tile-debug.apk' 'echo "=== Verifying ==="' '"$ADB" -s "$DEVICE" shell pm list packages | grep -E "worldance|mecool"' 'echo "Done. Reboot the Mecool, then open Melolo TV and accept Allow debugging (Always allow)."' > Melolo-Mecool-Backup/reinstall.sh && chmod +x Melolo-Mecool-Backup/reinstall.sh && zip -r Melolo-Mecool-Backup.zip Melolo-Mecool-Backup

To restore the keystore on a new Mac: copy debug.keystore from the backup to ~/.android/.

## Restore

1. Unzip the personal backup ZIP to ~/Desktop.
2. Open Terminal in the extracted Melolo-Mecool-Backup folder.
3. Run:

    ./reinstall.sh

4. Enter the Mecool's IP:PORT when prompted (IP:5555 works once the Mac is paired; otherwise
   pair first, see INSTALL.md).
5. Reboot the Mecool, open **Melolo TV**, accept "Allow debugging?" with "Always allow".

## Important

Do not disable the existing Google TV launcher unless you intentionally want to replace it.

## Architecture warning

This setup was tested on a Mecool Android TV 14 device reporting:

    armeabi-v7a,armeabi

Do not use the arm64_v8a Melolo split on this device; the earlier 5.4.8 package failed with INSTALL_FAILED_NO_MATCHING_ABIS.
