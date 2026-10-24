#!/bin/bash
# Opens Melolo in a freeform window covering the whole TV, so it fills the screen in landscape.
# Needs freeform windows enabled once (see README.md "Landscape"). Run after each Mecool reboot,
# or whenever Melolo shows as a narrow strip again. Afterwards the Melolo tile, Home and
# LTvLauncher all keep reusing this full-screen window.
set -e
ADB="${ADB:-$HOME/Desktop/platform-tools/adb}"
MELOLO=com.worldance.drama
ACTIVITY=com.worldance.novel.pages.splash.SplashActivity
WINDOWING_MODE_FREEFORM=5

DEVICE="$1"
if [ -z "$DEVICE" ]; then
    DEVICE=$("$ADB" devices | awk 'NR>1 && $2=="device" {print $1; exit}')
fi
if [ -z "$DEVICE" ]; then
    echo "No device connected. Run: $ADB connect IP:PORT" >&2
    exit 1
fi
A() { "$ADB" -s "$DEVICE" "$@"; }

if [ "$(A shell settings get global enable_freeform_support)" != "1" ]; then
    echo "Freeform windows are off. Enable them first (README.md, Landscape section)." >&2
    exit 1
fi

# An existing fullscreen Melolo task would be reused as-is, so close it first.
A shell am force-stop "$MELOLO"
A shell am start --windowingMode "$WINDOWING_MODE_FREEFORM" -n "$MELOLO/$ACTIVITY" >/dev/null

TASK=""
for _ in $(seq 1 20); do
    TASK=$(A shell am stack list | grep "$MELOLO" | grep -oE "taskId=[0-9]+" | head -1 | cut -d= -f2)
    [ -n "$TASK" ] && break
    sleep 0.5
done
if [ -z "$TASK" ]; then
    echo "Melolo did not start." >&2
    exit 1
fi

SIZE=$(A shell wm size | grep -oE "[0-9]+x[0-9]+" | tail -1)
A shell am task resize "$TASK" 0 0 "${SIZE%x*}" "${SIZE#*x}"
echo "Melolo is open full screen (task $TASK, ${SIZE})."
