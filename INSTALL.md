# Reinstall from the beginning

## 1. Prepare the Mecool
Enable Developer Options and Wireless debugging. Note the IP address and debugging port.

Pair when required:
adb pair IP:PAIRING_PORT

Connect:
adb connect IP:DEBUG_PORT

Verify:
adb devices

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

## 5. Install LTvLauncher
Place LTvLauncher-armeabi-v7a-release.apk on the Mac.

adb -s IP:DEBUG_PORT install ~/Desktop/LTvLauncher-armeabi-v7a-release.apk

Verify:
adb -s IP:DEBUG_PORT shell pm list packages | grep -i ltv

Expected: package:com.leanbitlab.ltvL

Launch:
adb -s IP:DEBUG_PORT shell monkey -p com.leanbitlab.ltvL 1

Use the Mecool remote to navigate to Melolo.

## 6. Keep Google TV launcher
Do not disable the original Google TV launcher during initial setup. LTvLauncher can be used alongside it.

## 7. If Melolo disappears from Google TV
This is expected for a phone-oriented app. Check that com.worldance.drama is still installed, then open LTvLauncher and select Melolo.

## 8. Known limitations
LTvLauncher solves launching/navigation of the launcher itself. It does not automatically convert Melolo's touch UI into D-pad controls. Portrait orientation may also remain an application-level behavior.

## 9. Removal
adb -s IP:DEBUG_PORT uninstall com.worldance.drama
adb -s IP:DEBUG_PORT uninstall com.leanbitlab.ltvL