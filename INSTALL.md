# Reinstall from the beginning

adb below means `~/Desktop/platform-tools/adb`.

## 1. Prepare the Mecool
Enable Developer Options and Wireless debugging. Note the IP address and debugging port.

Pair when required:
adb pair IP:PAIRING_PORT PAIRING_CODE

Connect:
adb connect IP:DEBUG_PORT

Once paired, the box also accepts:
adb connect IP:5555

Verify:
adb devices

Run pair/connect from your own Terminal app. If another tool (for example an AI assistant's
shell) gets "No route to host" while ping works from Terminal, macOS is blocking that tool from
the local network; start adb from Terminal and the other tool can use the running adb server.

## 2. Verify architecture
adb -s IP:DEBUG_PORT shell getprop ro.product.cpu.abilist

Expected for this setup: armeabi-v7a,armeabi

## 3. Prepare Melolo 4.9.2
Extract the legitimate Melolo 4.9.2 XAPK into ~/Desktop/Melolo-4.9.2/

Required files:
- com.worldance.drama.apk
- config.armeabi_v7a.apk
- config.xxhdpi.apk

Do not install config.arm64_v8a.apk on this 32-bit device.

## 4. Install Melolo
cd ~/Desktop/Melolo-4.9.2

adb -s IP:DEBUG_PORT install-multiple com.worldance.drama.apk config.armeabi_v7a.apk config.xxhdpi.apk

Expected result: Success

Verify:
adb -s IP:DEBUG_PORT shell pm list packages | grep worldance

Expected: package:com.worldance.drama

Open Melolo once and sign in to your account.

## 5. Enable freeform windows (one time)
Melolo only fills the TV in a freeform window. These two developer settings were unset before
(the default); they survive reboots.

adb -s IP:DEBUG_PORT shell settings put global enable_freeform_support 1
adb -s IP:DEBUG_PORT shell settings put global force_resizable_activities 1
adb -s IP:DEBUG_PORT reboot

Reconnect after the reboot (adb connect IP:5555).

## 6. Build and install the Melolo TV tile
See mecool-tv/README.md (Build from source) for the toolchain and the icon copy step. Then:

cd mecool-tv
./gradlew :melolo-tile:assembleDebug
./install.sh

Verify:
adb -s IP:DEBUG_PORT shell pm list packages | grep mecool

Expected: package:com.mecool.melolotile

## 7. First launch
On the Google TV home screen (or Apps), open **Melolo TV**.

The first time, the TV asks "Allow debugging?". Tick "Always allow" and press OK. The tile uses
the box's own adb (localhost:5555) to open Melolo in a full-screen freeform window. After that it
opens full screen every time, including after reboots, without the Mac.

## 8. Keep Google TV launcher
Do not disable the original Google TV launcher. LTvLauncher is no longer needed; if you keep it,
note that opening Melolo from it gives the narrow strip (use Melolo TV instead).

## 9. Known limitations
- Use a mouse or the remote's air-mouse mode inside Melolo. The D-pad does not drive Melolo's
  touch UI, and this box (no touchscreen) rejects simulated touches.
- When the tile has to reopen Melolo (after a reboot, or after leaving Melolo with Back), it takes
  2-3 seconds and Melolo's start screen shows again.
- force_resizable_activities applies to every app. If another app misbehaves, undo step 5.

## 10. Removal
adb -s IP:DEBUG_PORT uninstall com.mecool.melolotile
adb -s IP:DEBUG_PORT uninstall com.worldance.drama
adb -s IP:DEBUG_PORT uninstall com.leanbitlab.ltvL

Undo freeform (then reboot):
adb -s IP:DEBUG_PORT shell settings delete global enable_freeform_support
adb -s IP:DEBUG_PORT shell settings delete global force_resizable_activities
adb -s IP:DEBUG_PORT reboot
