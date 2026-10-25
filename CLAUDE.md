# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

- **App:** a START button begins a focus session, during which the phone is locked to the app via lock task mode. A −/+ picker under the button sets the session length (`FOCUS_DURATIONS_MINUTES`, 5 min to 3 h).
- **Stack:** single-module Android app (`:app`, package `com.example.hocuspocusfocus`), Kotlin + Jetpack Compose, minSdk 28, compile/target SDK 37.
- **Spec:** README.md is the user-facing spec (features, platform limits, device-owner setup); keep it in sync when behavior changes.

## Commands

```bash
./gradlew assembleDebug                                        # build
adb install -r app/build/outputs/apk/debug/app-debug.apk      # install on a device
./gradlew lint                                                 # Android lint
./gradlew testDebugUnitTest                                    # JVM unit tests (Robolectric)
./scripts/check.sh                                             # everything CI checks: build, lint, tests
adb shell dpm set-device-owner com.example.hocuspocusfocus/.FocusAdminReceiver  # enable full lock (device must have no accounts)
```

CI (`.github/workflows/ci.yml`) runs `scripts/check.sh` on pushes to `master` and PRs; so does `.githooks/pre-push` once enabled with `git config core.hooksPath .githooks` (the `/check` skill runs it too). Pushing a `v<versionName>` tag runs `release.yml` (which doesn't run the checks itself), which signs the release APK from repo secrets (passed to Gradle as `SIGNING_*` env vars, see README.md) and publishes a GitHub release; bump `versionName`/`versionCode` before tagging (the `/release` skill does all of this, and won't bump or tag unless `scripts/check.sh` passes). Unit tests are in `app/src/test`; `FocusViewModel` takes a `now` clock so tests drive the countdown with virtual time. Lock task mode can't be unit tested, so check it on the device (`/run`). Dependencies are declared in `gradle/libs.versions.toml`; Compose artifacts take their versions from the Compose BOM, so don't add versions to them.

## Architecture

The lock is driven by the timer state, not by button handlers:

- `FocusViewModel` owns the session. `remainingMillis: StateFlow<Long?>` is the single source of truth: `null` means focus mode is off. Only the session **end time** is persisted (SharedPreferences `focus` / `end_time`), so the countdown survives screen-off and activity/process recreation; on init an unexpired end time resumes the session. The selected `durationMinutes` is persisted too (`duration_minutes`).
- `MainActivity` collects `remainingMillis` while `RESUMED` (lock task can only start from the foreground) and calls `setFocusMode(active)` on changes, which toggles the lock and `FLAG_KEEP_SCREEN_ON`. `focusModeApplied` tracks what was last applied so resuming after the screen-pinning prompt doesn't re-lock, and a recreated activity re-locks an ongoing session. Volume keys are swallowed in `onKeyDown`/`onKeyUp` (overriding `dispatchKeyEvent` trips lint's `RestrictedApi` check, since `ComponentActivity`'s override is restricted); Back is swallowed with a `BackHandler`, since lock task mode still delivers it.
- `FocusLock` wraps lock task mode with two strengths:
  - **Device owner** (full lock): `applyPolicies()` whitelists the app via `setLockTaskPackages` and sets `LOCK_TASK_FEATURE_NONE` (no Home, Recents, shade, status bar info, power menu).
  - **Otherwise** `startLockTask()` falls back to screen pinning (soft lock), which the user must confirm and can undo. The UI shows a warning in this mode.
  - `releaseDeviceOwner()` (the "Remove device owner" button) is the only way to make the app uninstallable again after device-owner setup.
- `FocusAdminReceiver` is the empty `DeviceAdminReceiver` targeted by `dpm set-device-owner`; renaming or moving it breaks the adb command in README.md.
- `ui/FocusScreen.kt` is stateless: it renders idle (START, duration picker, device-owner controls/warning) vs. countdown (STOP) from `remainingMillis` and `isDeviceOwner`.

System bars are intentionally left visible during a session: lock task mode already empties them, and hiding them makes Android show a misleading "swipe down to exit" hint.
