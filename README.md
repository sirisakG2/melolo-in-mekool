# Melolo on Mecool Android TV 14

Working setup for running Melolo (com.worldance.drama) on a Mecool Android TV 14 box whose
reported ABI is 32-bit armeabi-v7a, full screen in landscape, opened from the Google TV home
screen.

Two parts:

1. **Melolo 5.1.6** itself, installed from its split APKs (see INSTALL.md). It is never modified.
2. **Melolo TV** tile (`mecool-tv/`), a small companion app built here. It puts a tile on the
   Google TV home screen and opens Melolo full screen in landscape instead of the narrow
   portrait strip. Your Melolo account and data stay inside Melolo.

## Tested device facts
- Android TV OS: 14 (user build)
- CPU ABI: armeabi-v7a,armeabi (no 64-bit ABI)
- Screen: 1920x1080, density 320
- No touchscreen / faketouch feature
- adbd also listens on TCP port 5555 (set by the firmware at every boot)
- Melolo package: com.worldance.drama
- Working Melolo version: 5.1.6 (version code 51617), the newest with a 32-bit armeabi-v7a split
- Previous working version: 4.9.2 (version code 49217); newer dramas don't play there
- Melolo TV tile package: com.mecool.melolotile (v2.1)

## Why Melolo 5.4.8 failed
The newer split package contained only an arm64_v8a native split, while this Mecool reports
only 32-bit armeabi-v7a. Installation failed with INSTALL_FAILED_NO_MATCHING_ABIS.

Melolo 5.1.6 (Feb 2026) still ships an armeabi_v7a split, so it is the newest version this box
can run. 4.9.2 also has one, but newer dramas (and favorites/share links to them) only open as a
"Read" (book) page or keep loading there; 5.1.6 plays them.

## Files used
Melolo 5.1.6 APKM bundle (APKMirror, variant "arm64-v8a + arm-v7a, 120-640dpi"):
- base.apk
- split_config.armeabi_v7a.apk
- split_config.arm64_v8a.apk (NOT installed on this Mecool)
- split_config.xhdpi.apk (matches the 320dpi screen)
- split_config.{ldpi,mdpi,tvdpi,hdpi,xxhdpi,xxxhdpi}.apk (not needed)

The installation used base.apk + split_config.armeabi_v7a.apk + split_config.xhdpi.apk, as an
update over 4.9.2 (same signer, POLIGON PTE. LTD., SHA-256 358331e8...41fbc), so login and data
were kept. APKMirror has since stopped offering Melolo downloads at the developer's request, so
keep your own copy (see BACKUP-AND-RESTORE.md).

Melolo 4.9.2 XAPK (previous version): com.worldance.drama.apk + config.armeabi_v7a.apk +
config.xxhdpi.apk.

Melolo TV tile: built from `mecool-tv/melolo-tile` (see `mecool-tv/README.md`).

LTvLauncher (LTvLauncher-armeabi-v7a-release.apk) is optional now; the tile replaces it.

## Important
This repository stores the reinstallation procedure, configuration and the tile's source code,
not third-party Melolo APK binaries or Melolo's icon. Obtain Melolo from a legitimate source and
verify that redistribution of any APK is permitted before putting APK files in a public repository.

## Quick restore
See INSTALL.md.

## Result
- Melolo 5.1.6 installed successfully (update over 4.9.2).
- Melolo is a phone app (LAUNCHER, not LEANBACK_LAUNCHER), so Google TV hides it. The Melolo TV
  tile is a TV app and shows on the Google TV home screen.
- Melolo is locked to portrait, so Android shows it as a 608px-wide strip. In a freeform window
  covering the whole TV it is not letterboxed and lays out wide, including wide-format dramas.
  The tile opens it that way by itself, also after a reboot.
- Use a mouse (or the remote's air-mouse mode) inside Melolo. The D-pad can't drive Melolo's
  touch UI well, and this box rejects simulated touches (see LESSONS-LEARNED.md).
- Google TV's original launcher was left intact.

You will see two icons: **Melolo** (the real app, opens as a strip) and **Melolo TV** (the tile,
opens the same Melolo full screen). Always use **Melolo TV**.

## Troubleshooting
Check architecture: adb -s DEVICE shell getprop ro.product.cpu.abilist
Check Melolo: adb -s DEVICE shell pm list packages | grep worldance
Resolve activity: adb -s DEVICE shell cmd package resolve-activity --brief com.worldance.drama
Launch Melolo: adb -s DEVICE shell monkey -p com.worldance.drama 1
Open full screen from the Mac: mecool-tv/landscape.sh
Tile logs: adb -s DEVICE logcat -s MeloloTile

More: `mecool-tv/README.md` (Troubleshooting) and LESSONS-LEARNED.md.

Do not disable the Google TV launcher unless you deliberately want to replace it.
