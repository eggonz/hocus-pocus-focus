# Hocus Pocus, Now You Focus

A minimal Android focus app: starts a focus session (5 minutes to 3 hours), and while it runs the phone is locked down so you can't escape into other apps.
Create custom routines with tasks and allowed apps, and the app will keep you on track.

Built with Kotlin and Jetpack Compose.

## Features

- **Big START button** starts focus mode with a countdown timer.
- **Duration picker** under the button: −/+ step through 5, 10, 15, 20 and 30 min, 1 h, 1 h 30 min, 2 h and 3 h (30 min by default; the last choice is remembered).
- **Pause/play and stop buttons** inside the countdown ring, under the time: pause freezes the timer (the phone stays locked) and play resumes it; stop asks for confirmation, then ends focus mode early.
- **Countdown ring** shows how much of the session is left.
- **While focus mode is active, the app blocks:**
  - Navigation bar: Home, Back, Recents (and gesture navigation)
  - Top bar: pulling down the notification shade and quick settings (full lock only)
  - Volume up / down / mute buttons
  - Power button: the long-press power menu (full lock only)

  See [How it works](#how-it-works) for the difference between the full and soft lock.
- The screen stays on during a session, also while an allowed app is open.
- When the timer reaches 0, focus mode ends, a short alarm sound plays once, and the phone works normally again. Stopping early plays nothing.
- When the app opens, it asks GitHub for the latest release. If that's newer than the installed version, a **New version available** prompt offers to download its APK in the browser (or waits for the end of the session, if one is running). Without a connection nothing is shown, and the prompt comes back the next time the app opens if it's dismissed.
- **Settings** (gear button, top left of the start screen; not reachable during a session):
  - **Alarm sound:** picked with the system sound picker from the notification sounds (the default notification sound unless changed; Silent turns it off). It plays at alarm volume and is cut off after 4 seconds, so a long sound stays a short notice.
  - **Lock:** whether the app has the full lock (device owner, with the **Remove device owner** button) or the soft lock.
  - **Share the app:** sends a link to the download page through the system share sheet.
  - The app version.
- **Routines** (sun-and-moon button above START; not reachable during a session): a grid of preset sessions, each with an icon, a title and a length. It starts with one, **Morning** (1 h, with nine sample tasks), and the + button at the top right adds a new one (untitled, 1 h, no allowed apps), opening it straight in the editor. Tapping a routine opens its page, and the pencil button at its top right opens an editor for its title, icon (sun, moon or book), length (set with −30/−5 and +5/+30 minute buttons, from 5 min to 3 h) allowed apps (a grid of app icons: 4 per row, up to 8, with the count next to the heading: the + at the end adds one from the installed apps, and each icon's red − badge removes it) and tasks (up to 20, with the count next to the heading: the + adds a row with an icon picker, a label field and an X that removes it). The routine's page shows how many tasks it has and, if it allows any apps, how many. Changes are saved as you make them, and the trash button at the top right of the editor deletes the routine after a confirmation. The **START** button at the bottom of a routine's page starts a session as long as the routine, and the countdown screen then shows the allowed apps' icons in a row that scrolls sideways (fading out at the edges where more are hidden): tapping one opens that app, Back returns to the countdown, and every other app stays locked. Without device owner this needs the **app guard** (see below): until it's on, the routine's page marks its allowed apps with a blocked symbol and shows a hint with a **Turn on** button above START, and a session started anyway shows the apps greyed out, and they can't be tapped or scrolled. If the timer runs out while an allowed app is open, the app comes back to the front and unlocks. If the routine has tasks, the countdown shrinks to the top of the screen, with its tasks below it as a grid of round icon buttons (5 per row) that toggle on and off when tapped, with no other effect (toggling one on briefly shows its label above it), and the allowed apps at the bottom of the screen.
- A dark violet and amber look in both the light and dark system themes, set in [Space Grotesk](https://github.com/floriankarsten/space-grotesk) and [DM Sans](https://github.com/googlefonts/dm-fonts) (bundled, SIL Open Font License, see `licenses/`).

## How it works

The app uses Android's **lock task mode**, in one of two strengths:

| | Full lock (device owner) | Soft lock (screen pinning) |
|---|---|---|
| Setup | One-time `adb` command, see below | None, but App pinning must be on in Settings |
| Start | Immediate | Android asks for confirmation ("App is pinned") |
| Home / Back / Recents | ✅ Blocked | ✅ Blocked |
| Notification shade / quick settings | ✅ Blocked, status bar is empty | ❌ Still available |
| Power menu (long press) | ✅ Blocked | ❌ Still available |
| Volume buttons | ✅ Blocked by the app | ✅ Blocked by the app |
| Way out before the timer ends | Only the STOP button | STOP button, or the system unpin gesture |

- **Full lock:** the app is set as the phone's **device owner**. It then whitelists itself for lock task mode and turns off every lock task feature (`DevicePolicyManager.setLockTaskFeatures(..., LOCK_TASK_FEATURE_NONE)`): no Home, Recents, notification shade, status bar info or power menu.
- **Soft lock:** without device owner, `startLockTask()` falls back to screen pinning. The settings screen says when it's in this mode.
- **Allowed apps:** a routine's apps are opened inside the app's own task, so Back comes back to the countdown. As device owner they're also added to the lock task whitelist for the session, so apps that always open in their own task (Spotify, for one) work too. An allowed app can still open other apps' screens inside the same task (a share sheet, say), since lock task mode only polices tasks.
- **App guard (without device owner):** screen pinning allows a single task, so it blocks every app that opens in its own. So a routine session with allowed apps isn't pinned when the **app guard** is on: an Accessibility service (Settings → Accessibility → Installed apps → Hocus Pocus app guard) that brings the app back whenever another app comes to the front during a session. It lets through the system UI (notification shade, lock screen, system dialogs), permission prompts, keyboards and call screens, and only looks at which app is in front, never at the screen's content. Since it blocks the Settings app too, it can't be turned off during a session. A non-allowed app may flash on screen for a moment before the guard sends you back. If the guard is off, the session falls back to screen pinning, and allowed apps that need their own task won't open. Sessions without allowed apps are pinned as usual, unless **Always use the app guard** is turned on in the settings (off by default; only shown without device owner): then they use the guard too, which is less strict than pinning but skips the system's "App is pinned" notice. While that switch is on and the guard is off, the main screen shows only the settings button and a prompt with a **Turn on** button. On Android 13+, an APK installed from a browser or file manager may show the guard's switch as a "Restricted setting": allow it first from the app's info page (⋮ → Allow restricted settings). Every **Turn on** button opens a help dialog with the steps: its **Open Settings** button goes to Accessibility, and on Android 13+, unless the app came from an app store, it also explains the restriction and has an **App info** button. The dialog closes by itself once the guard is on.
- **Volume keys:** the activity swallows volume key events while a session runs (`dispatchKeyEvent`).
- **Timer:** the session's end time is saved, so the countdown stays correct while the screen is off, and a session survives the app being recreated.

## Platform limits

- **Short power press:** no app can stop it from turning the screen off. When you turn the screen back on, you land back in the locked app.
- **Forced restart:** holding power + volume for several seconds force-restarts the phone at the hardware level, and that ends lock task mode. The timer state is kept, but the app doesn't relaunch on boot.
- **Emergency calls:** these always remain possible. Android guarantees this, and no app can block it.

## Build

Requirements: JDK 17+ and the Android SDK (platform 37). Android Studio works too: open the project folder.

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Minimum Android version: 9 (API 28).

## Checks

`./scripts/check.sh` runs what CI runs: the debug build, Android lint and the JVM unit tests (`app/src/test`, using Robolectric). On failure it lists the lint errors and failed tests.

To run it automatically before every push, enable the repo's hooks once per clone:

```bash
git config core.hooksPath .githooks
```

This also works when pushing from Android Studio. Skip it for one push with `git push --no-verify`, or untick **Run Git hooks** in Android Studio's push dialog.

## CI and releases

GitHub Actions runs `scripts/check.sh` (debug build, lint, unit tests) on every push to `master` and every pull request (`.github/workflows/ci.yml`). The APK and the lint and test reports are attached to the run as artifacts.

Pushing a `v*` tag builds a signed release APK and publishes it as a GitHub release (`.github/workflows/release.yml`), both versioned and as `hocus-pocus-focus.apk`, so the newest is always at `https://github.com/eggonz/hocus-pocus-focus/releases/latest/download/hocus-pocus-focus.apk`. The download page at `https://eggonz.github.io/hocus-pocus-focus/` links there and shows the latest version number: it's the static site in `docs/`, published with GitHub Pages (Settings → Pages → Deploy from a branch → `master`, `/docs`). It doesn't run the checks, so run `./scripts/check.sh` before tagging (the pre-push hook does this on push). The tag must match `versionName` in `app/build.gradle.kts`:

```bash
git tag v0.1.0 && git push origin v0.1.0
```

Release signing needs a keystore, stored as repository secrets (Settings → Secrets and variables → Actions). Create one once and keep it safe: updates to an installed app must be signed with the same key.

```bash
keytool -genkeypair -v -keystore release.jks -alias release -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 release.jks   # value for SIGNING_KEYSTORE_BASE64
```

| Secret | Value |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | The keystore, base64-encoded |
| `SIGNING_KEYSTORE_PASSWORD` | Keystore password |
| `SIGNING_KEY_ALIAS` | Key alias (`release` above) |
| `SIGNING_KEY_PASSWORD` | Key password |

To sign a release build locally, set the same variables, with `SIGNING_KEYSTORE_FILE` pointing at the keystore instead of the base64 value, and run `./gradlew assembleRelease`. A gitignored `signing.env` in the project root can hold them all:

```bash
set -a; . ./signing.env; set +a
./gradlew assembleRelease
```

## Enable the full lock (device owner)

Android only allows setting a device owner on a phone with **no accounts** on it (no Google account etc.). This usually means a freshly reset phone, or one where you temporarily remove all accounts in Settings → Accounts.

1. Enable USB debugging on the phone (Settings → About phone → tap *Build number* 7 times, then Settings → System → Developer options → USB debugging).
2. Install the app (see [Build](#build)).
3. Run:

   ```bash
   adb shell dpm set-device-owner com.eggonz.hocuspocusfocus/.FocusAdminReceiver
   ```

4. Re-add your accounts if you removed them.

If the command says there are accounts on the device right after a reboot, wait a few seconds and try again.

### Removing device owner

A device-owner app **can't be uninstalled** normally. To undo it, open the app (with no session running), open the settings (gear, top left) and tap **Remove device owner**. After that, the app uses the soft lock and can be uninstalled.

## Project structure

```
app/src/main/java/com/eggonz/hocuspocusfocus/
├── MainActivity.kt        # Hosts the UI; applies the lock, screen-on flag and volume blocking
├── FocusViewModel.kt      # Session timer, selected duration, alarm sound and routines, persisted in SharedPreferences
├── Routine.kt             # Routine model, its icons and JSON storage format
├── AlarmPlayer.kt         # Plays the alarm sound once when a session runs out
├── FocusLock.kt           # Lock task mode and device-owner policies
├── FocusAdminReceiver.kt  # Device admin component targeted by `dpm set-device-owner`
├── AppGuardService.kt     # Accessibility service keeping routine sessions to their allowed apps without device owner
├── UpdateChecker.kt       # Asks GitHub whether a newer release is out
└── ui/
    ├── HomeScreen.kt      # START button, duration picker, settings and routines buttons
    ├── FocusScreen.kt     # Countdown with pause/play and stop buttons, tasks and allowed apps
    ├── AppGuardHelpDialog.kt # Steps to turn on the app guard, with buttons to the settings pages
    ├── UpdateDialog.kt    # "New version available" prompt
    ├── SettingsScreen.kt  # Alarm sound, lock mode / device-owner removal, sharing, version
    ├── RoutinesScreen.kt  # Routines grid (preset sessions), with the add (+) button
    ├── RoutineScreen.kt   # One routine's page, with the edit (pencil) button
    ├── EditRoutineScreen.kt # Routine title, icon, length, allowed apps and tasks editor
    ├── AppIcon.kt         # Installed app model and icon, for a routine's allowed apps
    └── theme/
        ├── Theme.kt       # Material 3 theme: the app's own dark palette
        └── Type.kt        # Typography from the bundled fonts in res/font
app/src/test/java/com/eggonz/hocuspocusfocus/  # JVM unit tests (timer, alarm, countdown formatting)
scripts/check.sh                                # CI checks, also run by .githooks/pre-push
docs/                                           # Download page, published with GitHub Pages
licenses/                                       # Licenses and notices for the bundled fonts and icons
```

## License

The code is under the [MIT license](LICENSE). The bundled fonts and icons have their own licenses, and a few icons show third-party trademarks and characters that the MIT license doesn't cover: see [`licenses/THIRD_PARTY_NOTICES.md`](licenses/THIRD_PARTY_NOTICES.md).
