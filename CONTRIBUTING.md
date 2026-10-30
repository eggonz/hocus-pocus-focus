# Contributing

See the [README](README.md) for how to build the app.

## Checks

`./scripts/check.sh` runs what CI runs: the debug build, Android lint and the JVM unit tests (`app/src/test`, using Robolectric). On failure it lists the lint errors and failed tests.

To run it automatically before every push, enable the repo's hooks once per clone:

```bash
git config core.hooksPath .githooks
```

This also works when pushing from Android Studio. Skip it for one push with `git push --no-verify`, or untick **Run Git hooks** in Android Studio's push dialog.

## Project structure

```
app/src/main/java/com/eggonz/hocuspocusfocus/
├── MainActivity.kt        # Hosts the UI; applies the lock, screen-on flag and volume blocking
├── FocusViewModel.kt      # Session timer, settings and routines, persisted in SharedPreferences
├── Routine.kt             # Routine model and JSON storage format
├── AlarmPlayer.kt         # Plays the alarm sound once when a session runs out
├── FocusLock.kt           # Lock task mode and device-owner policies
├── FocusAdminReceiver.kt  # Device admin component targeted by `dpm set-device-owner`
├── AppGuardService.kt     # Accessibility service keeping sessions to their allowed apps without device owner
├── UpdateChecker.kt       # Asks GitHub whether a newer release is out
└── ui/                    # Stateless Compose screens and dialogs, theme
app/src/test/java/com/eggonz/hocuspocusfocus/  # JVM unit tests
scripts/check.sh                                # CI checks, also run by .githooks/pre-push
docs/                                           # Download page, published with GitHub Pages
licenses/                                       # Licenses and notices for the bundled fonts and icons
```

## CI and releases

GitHub Actions runs `scripts/check.sh` on every push to `master` and every pull request (`.github/workflows/ci.yml`). The APK and the lint and test reports are attached to the run as artifacts.

Pushing a `v*` tag builds a signed release APK and publishes it as a GitHub release (`.github/workflows/release.yml`), both versioned and as `hocus-pocus-focus.apk`, so the newest is always at `https://github.com/eggonz/hocus-pocus-focus/releases/latest/download/hocus-pocus-focus.apk`. The download page at `https://eggonz.github.io/hocus-pocus-focus/` links there and shows the latest version number: it's the static site in `docs/`, published with GitHub Pages (Settings → Pages → Deploy from a branch → `master`, `/docs`). The release workflow doesn't run the checks, so run `./scripts/check.sh` before tagging (the pre-push hook does this on push). The tag must match `versionName` in `app/build.gradle.kts`:

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
