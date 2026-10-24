# Lessons learned

What was tried on the Mecool Android TV 14 box and why things work the way they do.

## Melolo on Google TV
- Melolo declares only the normal LAUNCHER category, so Google TV hides it. A tiny companion
  app with LEANBACK_LAUNCHER and a banner (the Melolo TV tile) shows on the home screen and
  starts Melolo with `getLaunchIntentForPackage`. Android 11+ needs a `<queries>` entry for
  com.worldance.drama to see it.

## Melolo version
- Melolo moved dramas to a newer "series" format. In 4.9.2 those dramas (and favorites or share
  links pointing at them) open as a book page with only a "Read" button, or keep loading.
  Melolo 5.1.6 (last version with an armeabi-v7a split) plays them.
- A share link (www.melolo.org/s/...) redirects to /page/share-drama?sid=...&vid=...; the page's
  "Watch in Melolo" button builds https://melolodrama.onelink.me/OgQ9?...&series_id=...&video_id=...
  The `sid` there is an older id; the real one is `series_id`, and
  `worldance645713://seriesDetailPage?series_id=<id>` opens the drama page in Melolo.
- APKMirror stopped offering Melolo downloads at the developer's request (2026); keep a copy.

## Landscape
- Melolo's activities request SCREEN_ORIENTATION_PORTRAIT. In a normal (fullscreen) task the
  TV keeps landscape and letterboxes Melolo to 608x1080 in the middle. Wide-format dramas then
  play in a ~608x342 box. Melolo 4.9.2 has no full-screen button for them.
- This Android 14 build has no per-app "force full screen / any orientation" override
  (no OVERRIDE_ANY_ORIENTATION_TO_USER, no user aspect ratio setting).
- A **freeform** window is not letterboxed. With `enable_freeform_support=1` and
  `force_resizable_activities=1` (then reboot), `am start --windowingMode 5` plus
  `am task resize <task> 0 0 1920 1080` makes Melolo lay out wide and fill the TV, player
  included.
- `ActivityOptions.setLaunchBounds` alone leaves the task fullscreen (still letterboxed).
  `ActivityOptions.setLaunchWindowingMode` is hidden API; reflection fails with
  NoSuchMethodException. So an app can't request freeform itself.
- The freeform task is reused by later launches (tile, Home, LTvLauncher) until Melolo's task
  ends: reboot, force stop, or leaving Melolo with Back from its home screen.
- The firmware opens adbd on TCP 5555 at every boot (`service.adb.tcp.port=5555`). The tile
  therefore connects to `localhost:5555` with the dadb library (after a one-time "Allow
  debugging?" prompt) and runs the `am` commands itself. `mecool-tv/landscape.sh` does the same
  from the Mac as a backup.

## Remote control (helper app, later removed)
- The box reports no `android.hardware.touchscreen` or `faketouch` feature, so
  `AccessibilityService.dispatchGesture` is cancelled within milliseconds. Every simulated
  tap/swipe fails.
- Accessibility node actions do work: `ACTION_CLICK` on the clickable view under a pointer and
  `ACTION_SCROLL_FORWARD/BACKWARD` on the player's episode RecyclerView (next/previous episode).
- Melolo's video only pauses on a real touch; it ignores DPAD_CENTER, MEDIA_PLAY_PAUSE and SPACE.
- The remote (YICHIP receiver, also exposes a mouse) sends no key repeats while a button is
  held, so long presses must be timed.
- `adb shell input keyevent` bypasses accessibility key filters, so it can't test such a helper;
  real remote presses are needed.
- With the helper enabled, OK on Google TV home tiles behaved like a long press. It was
  uninstalled (and `enabled_accessibility_services` restored to the OEM service only) since a
  mouse covers everything. Source kept in `mecool-tv/melolo-remote` for reference.

## Mac side
- macOS Local Network privacy can block a tool's process from the LAN ("No route to host"
  while ping works in Terminal). Start the adb server from Terminal; other tools then go
  through it. Or allow the tool under System Settings > Privacy & Security > Local Network.
- Melolo blocks screenshots of the video player (screencap comes back empty), so player
  checks need someone looking at the TV.
