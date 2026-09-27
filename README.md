# Melolo on Mecool Android TV 14

Working setup for installing and launching Melolo (com.worldance.drama) on a Mecool Android TV 14 device whose reported 32-bit ABI is armeabi-v7a.

## Tested device facts
- Android TV OS: 14
- CPU ABI: armeabi-v7a,armeabi
- 64-bit ABI: none reported
- Physical density: 320
- Melolo package: com.worldance.drama
- Working Melolo version: 4.9.2 (version code 49217)
- LTvLauncher package: com.leanbitlab.ltvL

## Why Melolo 5.4.8 failed
The newer split package contained an arm64_v8a native split, while this Mecool reports only 32-bit armeabi-v7a. Installation failed with INSTALL_FAILED_NO_MATCHING_ABIS.

The working 4.9.2 package includes an armeabi_v7a split.

## Files used
Melolo 4.9.2 XAPK contents:
- com.worldance.drama.apk
- config.armeabi_v7a.apk
- config.arm64_v8a.apk (NOT installed on this Mecool)
- config.xxhdpi.apk
- config.xhdpi.apk
- icon.png
- manifest.json

The successful installation used com.worldance.drama.apk + config.armeabi_v7a.apk + config.xxhdpi.apk.

LTvLauncher: LTvLauncher-armeabi-v7a-release.apk

## Important
This repository stores the reinstallation procedure and configuration, not third-party Melolo APK binaries. Obtain Melolo from a legitimate source and verify that redistribution of any APK is permitted before putting APK files in a public repository.

## Quick restore
See INSTALL.md.

## Result
- Melolo installed successfully.
- Melolo was hidden from the standard Google TV launcher because it exposes a normal LAUNCHER category rather than LEANBACK_LAUNCHER.
- LTvLauncher was installed successfully and can launch Melolo using the TV remote.
- Google TV's original launcher was left intact.

## Troubleshooting
Check architecture: adb -s DEVICE shell getprop ro.product.cpu.abilist
Check Melolo: adb -s DEVICE shell pm list packages | grep worldance
Resolve activity: adb -s DEVICE shell cmd package resolve-activity --brief com.worldance.drama
Launch Melolo: adb -s DEVICE shell monkey -p com.worldance.drama 1
Launch LTvLauncher: adb -s DEVICE shell monkey -p com.leanbitlab.ltvL 1

Do not disable the Google TV launcher unless you deliberately want to replace it.