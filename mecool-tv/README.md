# Mecool TV add-ons for Melolo

A small tile app that opens the installed Melolo (`com.worldance.drama` 4.9.2) full screen in
landscape on the Mecool Android TV 14 box. It installs next to Melolo and never modifies it,
its data or your account. Uninstall it and Melolo is exactly as before. Use a mouse inside Melolo.

| App | Package | What it does |
|---|---|---|
| **Melolo TV** tile | `com.mecool.melolotile` | Puts a Melolo banner on the Google TV home screen. Pressing it opens the real Melolo (already signed in) full screen in landscape (see *Landscape*). No LTvLauncher needed. |
| ~~Melolo Remote~~ | `com.mecool.meloloremote` | **Removed from the box** (2026-10-24): not needed with a mouse, and while it was on, OK on home-screen tiles behaved like a long press. Source kept in `melolo-remote/` for reference; `install.sh` no longer installs it. |

## Landscape (full-TV Melolo)

Melolo is locked to portrait, so Android normally shows it as a narrow strip. In a
**freeform window** covering the whole TV, Android doesn't letterbox it, so Melolo lays out
wide and fills the screen.

One-time setup (two hidden developer settings, then reboot):

```bash
~/Desktop/platform-tools/adb shell settings put global enable_freeform_support 1
~/Desktop/platform-tools/adb shell settings put global force_resizable_activities 1
~/Desktop/platform-tools/adb reboot
```

**Open Melolo from the Melolo tile.** Apps can't request freeform themselves (the option is
hidden from apps), so the tile connects to the box's own adb on `localhost:5555` (the Mecool
keeps that port open, including after reboots) and runs the window commands itself:

- Melolo already in its full-screen window → the tile just brings it back.
- Otherwise (after a reboot, after leaving Melolo with Back, or if it was opened another way)
  → the tile closes Melolo and reopens it full screen. Takes 2–3 seconds; Melolo's start screen
  shows again.

The first time, the TV asks **"Allow debugging?"**: tick *Always allow* and press OK. The tile
then has adb (shell-level) access to the box; its key lives only in the app's private storage
and is removed if the tile is uninstalled. If the connection fails, the tile shows a message
and opens Melolo normally (as the strip).

Backup from the Mac, e.g. if a firmware update ever closes port 5555: connect adb and run
`./landscape.sh`, which does the same thing.

Undo (both were unset before; then reboot):

```bash
~/Desktop/platform-tools/adb shell settings delete global enable_freeform_support
~/Desktop/platform-tools/adb shell settings delete global force_resizable_activities
~/Desktop/platform-tools/adb reboot
```

`force_resizable_activities` affects every app, so if another app starts misbehaving, undo this.

## Install

Requirements on the Mac: `~/Desktop/platform-tools/adb`. The APK is built in
`melolo-tile/build/outputs/apk/debug/`.

1. On the Mecool: **Settings → System → Developer options → Wireless debugging → On**.
2. From the Mac's own Terminal (see *Troubleshooting* for why), pair and connect:

   ```bash
   ~/Desktop/platform-tools/adb pair 192.168.1.122:PAIRING_PORT PAIRING_CODE
   ~/Desktop/platform-tools/adb connect 192.168.1.122:DEBUG_PORT
   ```

3. Install the tile app:

   ```bash
   ./install.sh
   ```

4. Open Melolo from the **Melolo TV** tile and accept **"Allow debugging?"** (tick *Always allow*).

## Build from source

The tile uses Melolo's own icon, which is not stored in this repository. Copy it in first
from the Melolo 4.9.2 XAPK:

```bash
mkdir -p melolo-tile/src/main/res/drawable-nodpi
cp ~/Desktop/Melolo-4.9.2/icon.png melolo-tile/src/main/res/drawable-nodpi/melolo_icon.png
```

Then build:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
echo "sdk.dir=/opt/homebrew/share/android-commandlinetools" > local.properties
./gradlew :melolo-tile:assembleDebug
```

Toolchain (installed with Homebrew): `openjdk@17`, `gradle`, cask `android-commandlinetools`
with `platforms;android-34` and `build-tools;34.0.0` under
`/opt/homebrew/share/android-commandlinetools` (see `local.properties`).

The APKs contain no native code, so the 32-bit-only Mecool (`armeabi-v7a`) has no ABI issue.
They are signed with this Mac's debug key (`~/.android/debug.keystore`). **Back that file up**:
updating the apps later with a different key requires uninstalling them first.

## Backup and restore

Back up this `mecool-tv` folder plus `~/.android/debug.keystore`. To restore on a new Mac,
copy both back, then follow *Install*. Melolo itself is backed up separately
(see `../melolo-in-mekool-main/BACKUP-AND-RESTORE.md`).

## Uninstall

```bash
~/Desktop/platform-tools/adb uninstall com.mecool.melolotile
```

## Troubleshooting

- **`adb connect` says "No route to host" but `ping` works from Terminal**: macOS is blocking
  the calling app from the local network. Run `adb kill-server`, then pair/connect from your own
  Terminal; other tools can then use the running adb server. Permanent fix: **System Settings →
  Privacy & Security → Local Network** → allow the app.
- **The port changed**: Wireless debugging picks a new port after a reboot or toggle. Read it from
  the TV screen and `adb connect` again (pairing is remembered).
- **Reconnecting after a reboot**: the Mecool also listens on port 5555, so
  `adb connect 192.168.1.122:5555` works without reading a port off the TV.

## How it works (for maintenance)

- (Removed remote helper) The Mecool has no `android.hardware.touchscreen`/`faketouch` feature,
  so `AccessibilityService.dispatchGesture` is rejected; the helper used accessibility node
  actions (`ACTION_CLICK` / `ACTION_SCROLL_*`) instead. The remote sends no key repeats.
- The tile's `FreeformLauncher` uses the [dadb](https://github.com/mobile-dev-inc/dadb) library
  (Apache-2.0) to talk to adbd on `localhost:5555`, parses `am stack list` to see whether Melolo's
  task is freeform, and otherwise runs `am force-stop`, `am start --windowingMode 5` and
  `am task resize`. Logs: `adb logcat -s MeloloTile`.
