# CLAUDE.md

Focus app: START begins a session (5 min–3 h) that locks the phone to the app via lock task mode; routines add allowed apps. Single-module Android app (`:app`, package `com.example.hocuspocusfocus`), Kotlin + Compose, minSdk 28, target SDK 37. README.md is the user-facing spec.

## Working efficiently

- Small task, small effort: open only the files the change touches (the map below says where things are). Don't explore the codebase, read README.md or re-read files you just edited.
- Never install or launch the app unless asked; the user runs `/run`.
- Run `./scripts/check.sh` once when the change is done, not after every edit. It prints nothing on success.
- Update this file or README.md only when user-visible behavior or architecture changes, not for layout, styling or wording tweaks.

## Commands

- `./scripts/check.sh`: build, lint and unit tests, as CI and the pre-push hook do (`/check` runs it).
- `./gradlew testDebugUnitTest --tests '*FocusViewModelTest'`: one test class.
- Releases: `/release` (bumps the version, tags, pushes; `release.yml` signs and publishes).
- Dependencies are in `gradle/libs.versions.toml`; Compose artifacts take versions from the BOM.

## Map and non-obvious constraints

- `FocusViewModel`: owns the session, settings and routines (all in SharedPreferences `focus`). `remainingMillis == null` means focus mode is off; only the end time is persisted, so a session survives process death. Takes injectable `now` and `playAlarm` for tests.
- `MainActivity`: applies the lock from `remainingMillis` while `RESUMED` (lock task needs the foreground); `focusModeApplied` stops re-locking after the pinning prompt. Holds the navigation flags (`showSettings`, `showRoutines`, `openRoutineId`, `isEditingRoutine`, idle only) and loads installed apps. `openApp()` strips `FLAG_ACTIVITY_NEW_TASK` so apps open inside the locked task. Volume keys are swallowed in `onKeyDown`/`onKeyUp` (overriding `dispatchKeyEvent` trips lint `RestrictedApi`), Back with a `BackHandler`.
- `FocusLock`: device owner gives the full lock (`LOCK_TASK_FEATURE_NONE`, routine apps whitelisted); otherwise screen pinning (soft lock), which blocks apps that open in their own task, so sessions with allowed apps aren't pinned while the app guard is on (nor any session with "Always use the app guard"). `releaseDeviceOwner()` is the only way to make the app uninstallable again.
- `AppGuardService`: accessibility service that brings the app back unless the front app is allowed (routine apps, system UI, keyboards, call screens).
- `FocusAdminReceiver`: target of `dpm set-device-owner`; renaming it breaks the README command.
- `Routine.kt`: routine model and JSON. `AlarmPlayer.kt`: end-of-session sound.
- `ui/`: stateless screens (values in, callbacks out): `HomeScreen` (idle), `FocusScreen` (countdown), `SettingsScreen`, `RoutinesScreen` (grid), `RoutineScreen` (page), `EditRoutineScreen`, `AppIcon` (`InstalledApp`), `IconPool` (enum of every routine icon, persisted by name, and the quick-pick lists).
- System bars stay visible during a session on purpose: hiding them makes Android show a misleading "swipe down to exit" hint.

## Code style

- Official Kotlin style, 120 columns, trailing commas in multi-line lists; prefer `val` and expression bodies.
- Full-word names; booleans read as questions (`isDeviceOwner`).
- Comments explain why, briefly; public declarations get a one-line KDoc. No commented-out code or debug logging.
- Compose: state lives in `FocusViewModel` or `MainActivity`; each screen has `@Preview`s; text in `strings.xml`, colors from the theme.
- Smallest change that does the job; reuse helpers; remove what the change leaves unused.
- Fix warnings instead of suppressing; an unavoidable suppression is narrow with the reason on the same line.
- `FocusViewModel` behavior changes come with unit tests (virtual time via `now`, no `Thread.sleep`).
