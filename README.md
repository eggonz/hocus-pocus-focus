# Hocus Pocus, Now You Focus

A minimal Android focus app. Start a session of 5 minutes to 3 hours, and the phone locks down so you can't escape into other apps. Routines add a task list and the few apps you do need.

**[Download the latest APK](https://eggonz.github.io/hocus-pocus-focus/)** (Android 9 or newer)

## Features

- **Focus sessions:** a big START button, a duration picker (5 min to 3 h) and a countdown ring. Pause freezes the timer while the phone stays locked; stop asks for confirmation.
- **Lockdown:** Home, Back, Recents, gestures and the volume buttons are blocked during a session, and with the full lock also the notification shade and the power menu. The screen stays on.
- **Routines:** preset sessions with an icon, a length, up to 8 allowed apps and up to 20 tasks to tick off. Changes save as you make them.
- **Alarm:** a short sound at the end of a session, picked from the system's notification sounds, or silent. Stopping early plays nothing.
- **Updates:** when the app opens, it asks GitHub for the latest release and offers the new APK if there is one. It sends nothing else, and without a connection it stays quiet.

## Install

Download the APK from the [download page](https://eggonz.github.io/hocus-pocus-focus/) and open it, allowing your browser to install apps if asked. No setup is needed for the soft lock; the [full lock](#enable-the-full-lock) takes one `adb` command.

## Lock modes

The app uses Android's **lock task mode**, in one of two strengths. The settings screen shows which one is active.

| | Full lock (device owner) | Soft lock (screen pinning) |
|---|---|---|
| Setup | One-time `adb` command | None, but App pinning must be on in Settings |
| Start | Immediate | Android asks for confirmation ("App is pinned") |
| Home / Back / Recents | ✅ Blocked | ✅ Blocked |
| Notification shade / quick settings | ✅ Blocked, status bar is empty | ❌ Still available |
| Power menu (long press) | ✅ Blocked | ❌ Still available |
| Volume buttons | ✅ Blocked by the app | ✅ Blocked by the app |
| Way out before the timer ends | Only the STOP button | STOP button, or the system unpin gesture |

### Allowed apps and the app guard

A routine's apps open from the countdown screen, Back returns to it, and every other app stays locked. If the timer runs out while one is open, the app comes back to the front and unlocks. An allowed app can still open other apps' screens inside the same task (a share sheet, say), since lock task mode only polices tasks.

With the full lock this just works. Screen pinning allows a single task, though, so it blocks every app that opens in its own (Spotify, for one). Without device owner, sessions with allowed apps therefore need the **app guard**: an Accessibility service that brings the app back whenever another app comes to the front.

- **Turning it on:** every **Turn on** button in the app opens a dialog with the steps (Settings → Accessibility → Installed apps → Hocus Pocus app guard). On Android 13+, an APK installed from a browser may show the switch as a "Restricted setting": allow it first from the app's info page (⋮ → Allow restricted settings).
- **Privacy:** it only looks at which app is in front, never at the screen's content.
- **What gets through:** the system UI (notification shade, lock screen, system dialogs), permission prompts, keyboards and call screens.
- **Limits:** a blocked app may flash on screen for a moment before the guard sends you back. It blocks the Settings app too, so it can't be turned off during a session.
- **If it's off:** the session falls back to screen pinning, and allowed apps that need their own task won't open.

Sessions without allowed apps are pinned as usual, unless **Always use the app guard** is on in the settings. That's less strict than pinning, but skips the system's "App is pinned" notice.

### Platform limits

- **Short power press:** no app can stop it from turning the screen off. When the screen comes back on, you land back in the locked app.
- **Forced restart:** holding power + volume for several seconds restarts the phone at the hardware level, which ends lock task mode. The timer state is kept, but the app doesn't relaunch on boot.
- **Emergency calls:** these always remain possible. Android guarantees this, and no app can block it.

## Enable the full lock

Android only allows setting a device owner on a phone with **no accounts** on it. This usually means a freshly reset phone, or one where you temporarily remove all accounts in Settings → Accounts.

1. Enable USB debugging on the phone (Settings → About phone → tap *Build number* 7 times, then Settings → System → Developer options → USB debugging).
2. Install the app.
3. Run:

   ```bash
   adb shell dpm set-device-owner com.eggonz.hocuspocusfocus/.FocusAdminReceiver
   ```

4. Re-add your accounts if you removed them.

If the command says there are accounts on the device right after a reboot, wait a few seconds and try again.

**To undo it:** a device-owner app can't be uninstalled normally. Open the app with no session running, open the settings (gear, top left) and tap **Remove device owner**. After that, the app uses the soft lock and can be uninstalled.

## Build from source

Requirements: JDK 17+ and the Android SDK (platform 37). Android Studio works too: open the project folder.

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Checks, CI and releases are described in [CONTRIBUTING.md](CONTRIBUTING.md).

## License

The code is under the [MIT license](LICENSE). The bundled fonts ([Space Grotesk](https://github.com/floriankarsten/space-grotesk) and [DM Sans](https://github.com/googlefonts/dm-fonts), SIL Open Font License) and icons have their own licenses, and a few icons show third-party trademarks and characters that the MIT license doesn't cover: see [`licenses/THIRD_PARTY_NOTICES.md`](licenses/THIRD_PARTY_NOTICES.md).
