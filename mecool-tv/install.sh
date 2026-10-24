#!/bin/bash
# Installs (or updates) the Melolo tile app on the connected Mecool.
# Connect first: adb pair / adb connect (see README.md).
set -e
cd "$(dirname "$0")"
ADB="${ADB:-$HOME/Desktop/platform-tools/adb}"

DEVICE="$1"
if [ -z "$DEVICE" ]; then
    DEVICE=$("$ADB" devices | awk 'NR>1 && $2=="device" {print $1; exit}')
fi
if [ -z "$DEVICE" ]; then
    echo "No device connected. Run: $ADB connect IP:PORT" >&2
    exit 1
fi
echo "=== Device: $DEVICE"

# melolo-remote is no longer installed (see README.md).
for apk in melolo-tile/build/outputs/apk/debug/melolo-tile-debug.apk; do
    echo "=== Installing $apk"
    "$ADB" -s "$DEVICE" install -r "$apk"
done

echo "=== Installed:"
"$ADB" -s "$DEVICE" shell pm list packages | grep -E "worldance|mecool"
echo "Done. Open Melolo from the Melolo tile; accept \"Allow debugging?\" on the TV the first time."
