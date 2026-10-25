# Hocus Pocus, Now You Focus

A minimal Android focus app: one big button starts a 30-minute focus session, and while it runs the phone is locked down so you can't escape into other apps.

Built with Kotlin and Jetpack Compose.

## Features

- **Big START button** starts focus mode with a **30:00** countdown timer.
- **Small STOP button** ends focus mode early.
- **While focus mode is active, the app blocks:**
  - Navigation bar: Home, Back, Recents (and gesture navigation)
  - Top bar: pulling down the notification shade and quick settings (full lock only)
  - Volume up / down / mute buttons
  - Power button: the long-press power menu (full lock only)

  See [How it works](#how-it-works) for the difference between the full and soft lock.
- The screen stays on during a session.
- When the timer reaches 0, focus mode ends and the phone works normally again.

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
- **Soft lock:** without device owner, `startLockTask()` falls back to screen pinning. The app shows a warning on its start screen when it's in this mode.
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

Pushing a `v*` tag builds a signed release APK and publishes it as a GitHub release (`.github/workflows/release.yml`). It doesn't run the checks, so run `./scripts/check.sh` before tagging (the pre-push hook does this on push). The tag must match `versionName` in `app/build.gradle.kts`:

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
   adb shell dpm set-device-owner com.example.hocuspocusfocus/.FocusAdminReceiver
   ```

4. Re-add your accounts if you removed them.

If the command says there are accounts on the device right after a reboot, wait a few seconds and try again.

### Removing device owner

A device-owner app **can't be uninstalled** normally. To undo it, open the app (with no session running) and tap **Remove device owner** at the bottom of the start screen. After that, the app uses the soft lock and can be uninstalled.

## Project structure

```
app/src/main/java/com/example/hocuspocusfocus/
├── MainActivity.kt        # Hosts the UI; applies the lock, screen-on flag and volume blocking
├── FocusViewModel.kt      # 30-minute timer, end time persisted in SharedPreferences
├── FocusLock.kt           # Lock task mode and device-owner policies
├── FocusAdminReceiver.kt  # Device admin component targeted by `dpm set-device-owner`
└── ui/
    ├── FocusScreen.kt     # START button, countdown and STOP button
    └── theme/Theme.kt     # Material 3 theme
app/src/test/java/com/example/hocuspocusfocus/  # JVM unit tests (timer, countdown formatting)
scripts/check.sh                                # CI checks, also run by .githooks/pre-push
```
