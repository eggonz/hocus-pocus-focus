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

CI (`.github/workflows/ci.yml`) runs `scripts/check.sh` on pushes to `master` and PRs; so does `.githooks/pre-push` once enabled with `git config core.hooksPath .githooks` (the `/check` skill runs it too). Pushing a `v<versionName>` tag runs `release.yml` (which doesn't run the checks itself), which signs the release APK from repo secrets (passed to Gradle as `SIGNING_*` env vars, see README.md) and publishes a GitHub release; bump `versionName`/`versionCode` before tagging (the `/release` skill does all of this, and won't bump or tag unless `scripts/check.sh` passes). Unit tests are in `app/src/test`; `FocusViewModel` takes a `now` clock so tests drive the countdown with virtual time. Lock task mode can't be unit tested; the user checks it on the device with `/run`. Dependencies are declared in `gradle/libs.versions.toml`; Compose artifacts take their versions from the Compose BOM, so don't add versions to them.

## Architecture

The lock is driven by the timer state, not by button handlers:

- `FocusViewModel` owns the session. `remainingMillis: StateFlow<Long?>` is the single source of truth: `null` means focus mode is off. Only the session **end time** is persisted (SharedPreferences `focus` / `end_time`), so the countdown survives screen-off and activity/process recreation; on init an unexpired end time resumes the session. `startRoutine()` starts a session as long as a routine and keeps it as `sessionRoutine` (persisted as `routine_id`, cleared with the end time), whose allowed apps the countdown shows. The selected `durationMinutes` is persisted too (`duration_minutes`; v0.2.0 saved `duration_seconds`, still read as a fallback), and so is `alarmSound` (`alarm_sound`: absent means the system default notification sound, `""` means silent), and so are the `routines` (`routines`, a JSON list, see `Routine.kt`; absent means the built-in 1 h Morning routine; `updateRoutine()` saves an edit and `addRoutine()` appends a new 1 h one with the next free id, `deleteRoutine()` removes one). When a countdown runs out by itself (not on `stop()`), the alarm sound is passed to the injectable `playAlarm`, which defaults to `AlarmPlayer` (plays it once on the alarm stream, cut off after 4 s).
- `MainActivity` collects `remainingMillis` while `RESUMED` (lock task can only start from the foreground) and calls `setFocusMode(active)` on changes, which toggles the lock and `FLAG_KEEP_SCREEN_ON`. `focusModeApplied` tracks what was last applied so resuming after the screen-pinning prompt doesn't re-lock, and a recreated activity re-locks an ongoing session. `openApp()` opens an allowed app with `FLAG_ACTIVITY_NEW_TASK` stripped, so it lands in this (locked) task, which screen pinning allows and Back returns from; a session that ends while the activity isn't resumed relaunches it (it's `singleTask`, so that clears the app on top) so it can unlock. Volume keys are swallowed in `onKeyDown`/`onKeyUp` (overriding `dispatchKeyEvent` trips lint's `RestrictedApi` check, since `ComponentActivity`'s override is restricted); Back is swallowed with a `BackHandler`, since lock task mode still delivers it.
- `FocusLock` wraps lock task mode with two strengths:
  - **Device owner** (full lock): `applyPolicies()` whitelists the app via `setLockTaskPackages` and sets `LOCK_TASK_FEATURE_NONE` (no Home, Recents, shade, status bar info, power menu). `lock(allowedApps)` adds the session routine's apps to the whitelist, and `unlock()` resets it.
  - **Otherwise** `startLockTask()` falls back to screen pinning (soft lock), which the user must confirm and can undo. The UI shows a warning in this mode. Pinning allows one task, so it blocks allowed apps that open in their own (most `singleTask` launchers); a session with allowed apps is therefore not pinned while the app guard is on. Nor is one without, if the user turned on "Always use the app guard" (`alwaysUseAppGuard`, persisted as `always_use_app_guard`, default off), which skips pinning's "App is pinned" notice.
- `AppGuardService` (the "app guard") is an `AccessibilityService` the user turns on in the system settings. On each window change during a session it brings `MainActivity` back unless the app in front is allowed: the session routine's apps (read from the prefs with `FocusViewModel.savedSessionAllowedApps()`, since it has no view model) or the system UI, permission controller, keyboards and call screens (`InCallService` packages, hence a second `<queries>` entry). `AppGuardService.isOn()` is checked on every resume, for the routine page's "Turn on" hint.
  - `releaseDeviceOwner()` (the "Remove device owner" button) is the only way to make the app uninstallable again after device-owner setup.
- `FocusAdminReceiver` is the empty `DeviceAdminReceiver` targeted by `dpm set-device-owner`; renaming or moving it breaks the adb command in README.md.
- `ui/FocusScreen.kt` is stateless: it renders idle (START, duration picker, settings and routines buttons, or only the settings button and a "Turn on" app guard prompt while `alwaysUseAppGuard` is on and the guard isn't) vs. countdown (the session routine's allowed apps, greyed out and not tappable or scrollable unless `canOpenApps`, i.e. device owner or the app guard on; STOP) from `remainingMillis`.
- `ui/SettingsScreen.kt` shows the alarm sound (picked with the system `ACTION_RINGTONE_PICKER`, launched by `MainActivity`), the lock strength with the "Remove device owner" button or the soft-lock warning and the "Always use the app guard" switch (with a "Turn on" button while the guard is off), and `BuildConfig.VERSION_NAME`. `MainActivity` switches to it with a `showSettings` flag, only while no session runs.
- `ui/RoutinesScreen.kt` shows a grid of `Routine`s (icon, title, duration), with a plus button at the top right that adds a routine and opens it in the editor. Tapping one opens `ui/RoutineScreen.kt` (its page, with a pencil button at the top right and a START button, and a blocked symbol by the allowed app count with the app guard hint), which opens `ui/EditRoutineScreen.kt` (title, `RoutineIcon`, duration and allowed apps, saved on every change, and a trash button at the top right that deletes it after a confirmation dialog; the duration has its own −30/−5/+5/+30 minute picker, clamped to 5 min–3 h by `stepRoutineDuration`). A routine's `allowedApps` are package names; `MainActivity` loads the launchable apps as `InstalledApp`s (`ui/AppIcon.kt`, label and an icon drawn at its display size) in the background at start, so they are ready when a routine opens, and reloads them each time the routines open, which needs the launcher `<queries>` in the manifest, and the screens show only the allowed apps that are still installed. `MainActivity` switches between them with the `showRoutines`, `openRoutineId` and `isEditingRoutine` flags, also only while no session runs.

System bars are intentionally left visible during a session: lock task mode already empties them, and hiding them makes Android show a misleading "swipe down to exit" hint.

## Code style

Code you write should read like the code already here: clear first, clever never.

- **Kotlin:** follow the official Kotlin style. Lines stay within 120 columns (`.editorconfig`); multi-line argument and parameter lists use trailing commas. Prefer `val`, immutable state and expression bodies where they read naturally.
- **Names** say what a thing is or does, in full words (`remainingMillis`, not `rem`); booleans read as questions (`isDeviceOwner`).
- **Comments** explain *why* (a platform quirk, a non-obvious constraint), not *what* the code already says; keep them short. Public classes and functions get a one-line KDoc. No commented-out code, TODOs without context, or leftover debug logging.
- **Compose:** screens in `ui/` stay stateless (values in, callbacks out); state lives in `FocusViewModel` or is hoisted to `MainActivity`. Each screen has `@Preview`s. User-visible text goes in `strings.xml` and colors come from the theme, never hard-coded.
- **Scope:** make the smallest change that does the job. Reuse existing helpers before adding new ones; don't add abstractions, options or dependencies the task doesn't need; remove imports, resources and code that a change leaves unused.
- **Warnings:** fix lint and compiler warnings instead of suppressing them. If a suppression is unavoidable, scope it to the narrowest element and say why on the same line (as in `FocusLock.releaseDeviceOwner`).
- **Tests:** a change to `FocusViewModel` behavior comes with unit tests; inject time, I/O and side effects (like `now` and `playAlarm`) so tests stay deterministic.
- **Done means checked:** run `./scripts/check.sh` once before calling a change finished (not after every edit), and update this file and README.md only when user-visible behavior or architecture changes (not for layout, styling or wording tweaks).

## Working efficiently

- Keep small tasks small: use the Architecture section above as the map and open only the files the change touches, instead of exploring the codebase or reading README.md.
- Never install or launch the app on the phone unless the user asks (they run `/run` themselves) or it is necessary for a certain fix.
