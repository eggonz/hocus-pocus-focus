package com.eggonz.hocuspocusfocus

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.content.IntentCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.eggonz.hocuspocusfocus.ui.APP_ICON_SIZE
import com.eggonz.hocuspocusfocus.ui.AppGuardHelpDialog
import com.eggonz.hocuspocusfocus.ui.EditRoutineScreen
import com.eggonz.hocuspocusfocus.ui.FocusScreen
import com.eggonz.hocuspocusfocus.ui.HomeScreen
import com.eggonz.hocuspocusfocus.ui.InstalledApp
import com.eggonz.hocuspocusfocus.ui.RoutineScreen
import com.eggonz.hocuspocusfocus.ui.RoutinesScreen
import com.eggonz.hocuspocusfocus.ui.SettingsScreen
import com.eggonz.hocuspocusfocus.ui.UpdateDialog
import com.eggonz.hocuspocusfocus.ui.appsIn
import com.eggonz.hocuspocusfocus.ui.theme.HocusPocusFocusTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// The page in docs/, which always offers the latest APK
private const val DOWNLOAD_PAGE_URL = "https://eggonz.github.io/hocus-pocus-focus/"

class MainActivity : ComponentActivity() {

    private val viewModel: FocusViewModel by viewModels()
    private lateinit var focusLock: FocusLock

    // What was last applied to the window and lock task, so that resuming (e.g. after the screen
    // pinning prompt) doesn't lock again, and an ongoing session is re-locked after recreation
    private var focusModeApplied = false

    // Keeps the screen on while an allowed app is in front, where this window's FLAG_KEEP_SCREEN_ON doesn't count
    @Suppress("DEPRECATION") // The replacement, FLAG_KEEP_SCREEN_ON, can't cover another app's window
    private val allowedAppWakeLock by lazy {
        getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "hocuspocusfocus:allowed_app")
            .apply { setReferenceCounted(false) }
    }

    // Checked on every resume, since the user turns it on in the system settings
    private var isAppGuardOn by mutableStateOf(false)

    // The newer release found when the app opened, until the user answers the prompt
    private var availableUpdateVersion by mutableStateOf<String?>(null)

    private val isFocusActive: Boolean
        get() = viewModel.remainingMillis.value != null

    // The system sound picker; a null pick means Silent
    private val pickAlarmSound = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {
            viewModel.setAlarmSound(IntentCompat.getParcelableExtra(data, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark, so the bar icons stay light whatever the system theme
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        focusLock = FocusLock(this).apply { applyPolicies() }

        // Lock task can only be started from the foreground, so focus mode follows the timer
        // while resumed; a session that ended with the screen off is unlocked on return
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.remainingMillis
                    .map { it != null }
                    .distinctUntilChanged()
                    .collect { active -> if (active != focusModeApplied) setFocusMode(active) }
            }
        }
        // A session that runs out while an allowed app is in front brings this app back, which then unlocks
        lifecycleScope.launch {
            viewModel.remainingMillis.collect { remaining ->
                val isResumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                if (remaining == null && focusModeApplied && !isResumed) {
                    releaseAllowedAppWakeLock()
                    startActivity(Intent(this@MainActivity, MainActivity::class.java))
                }
            }
        }

        // Only when the app opens, not when the activity is recreated
        if (savedInstanceState == null) {
            lifecycleScope.launch { availableUpdateVersion = newerReleasedVersion(BuildConfig.VERSION_NAME) }
        }

        setContent {
            HocusPocusFocusTheme {
                val remainingMillis by viewModel.remainingMillis.collectAsStateWithLifecycle()
                val durationMinutes by viewModel.durationMinutes.collectAsStateWithLifecycle()
                val alarmSound by viewModel.alarmSound.collectAsStateWithLifecycle()
                val routines by viewModel.routines.collectAsStateWithLifecycle()
                val sessionRoutine by viewModel.sessionRoutine.collectAsStateWithLifecycle()
                val alwaysUseAppGuard by viewModel.alwaysUseAppGuard.collectAsStateWithLifecycle()
                val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()
                var isDeviceOwner by remember { mutableStateOf(focusLock.isDeviceOwner) }
                var showSettings by rememberSaveable { mutableStateOf(false) }
                var showRoutines by rememberSaveable { mutableStateOf(false) }
                var openRoutineId by rememberSaveable { mutableStateOf<Int?>(null) }
                var isEditingRoutine by rememberSaveable { mutableStateOf(false) }
                var showAppGuardHelp by rememberSaveable { mutableStateOf(false) }
                val openRoutine = routines.find { it.id == openRoutineId }
                val openRoutineAppCount = remember(openRoutine?.allowedApps) {
                    openRoutine?.allowedApps.orEmpty().count { packageManager.getLaunchIntentForPackage(it) != null }
                }
                // Loaded off the main thread at start, so the icons are ready when a routine opens, and again each
                // time the routines open to pick up newly installed apps
                val installedApps by produceState(emptyList(), showRoutines) {
                    if (showRoutines || value.isEmpty()) value = withContext(Dispatchers.Default) { launchableApps() }
                }

                // Lock task mode still delivers Back to the app
                BackHandler(enabled = remainingMillis != null) {}

                // Settings and routines are only reachable between sessions
                if (showRoutines && openRoutine != null && isEditingRoutine && remainingMillis == null) {
                    BackHandler { isEditingRoutine = false }
                    EditRoutineScreen(
                        routine = openRoutine,
                        allowedAppCount = openRoutineAppCount,
                        installedApps = installedApps,
                        onChange = viewModel::updateRoutine,
                        onDelete = {
                            viewModel.deleteRoutine(openRoutine.id)
                            openRoutineId = null
                            isEditingRoutine = false
                        },
                        onBack = { isEditingRoutine = false },
                    )
                } else if (showRoutines && openRoutine != null && remainingMillis == null) {
                    BackHandler { openRoutineId = null }
                    RoutineScreen(
                        routine = openRoutine,
                        allowedAppCount = openRoutineAppCount,
                        onEdit = { isEditingRoutine = true },
                        onStart = { viewModel.startRoutine(openRoutine) },
                        showAppGuardHint = !isDeviceOwner && !isAppGuardOn && openRoutineAppCount > 0,
                        onTurnOnAppGuard = { showAppGuardHelp = true },
                        onBack = { openRoutineId = null },
                    )
                } else if (showRoutines && remainingMillis == null) {
                    BackHandler { showRoutines = false }
                    RoutinesScreen(
                        routines = routines,
                        onOpenRoutine = { openRoutineId = it },
                        // A new routine opens straight in the editor, since it has nothing to start yet
                        onAddRoutine = {
                            openRoutineId = viewModel.addRoutine().id
                            isEditingRoutine = true
                        },
                        onBack = { showRoutines = false },
                    )
                } else if (showSettings && remainingMillis == null) {
                    BackHandler { showSettings = false }
                    SettingsScreen(
                        alarmSoundTitle = remember(alarmSound) { alarmSoundTitle(alarmSound) },
                        isDeviceOwner = isDeviceOwner,
                        alwaysUseAppGuard = alwaysUseAppGuard,
                        isAppGuardOn = isAppGuardOn,
                        versionName = BuildConfig.VERSION_NAME,
                        onPickAlarmSound = { pickAlarmSound.launch(alarmSoundPickerIntent(alarmSound)) },
                        onAlwaysUseAppGuardChange = viewModel::setAlwaysUseAppGuard,
                        onTurnOnAppGuard = { showAppGuardHelp = true },
                        onRemoveDeviceOwner = {
                            focusLock.releaseDeviceOwner()
                            isDeviceOwner = false
                        },
                        onShareApp = ::shareApp,
                        onBack = { showSettings = false },
                    )
                } else {
                    val sessionRemainingMillis = remainingMillis
                    if (sessionRemainingMillis == null) {
                        HomeScreen(
                            durationMinutes = durationMinutes,
                            onStepDuration = viewModel::stepDuration,
                            onStart = viewModel::start,
                            onOpenSettings = { showSettings = true },
                            onOpenRoutines = { showRoutines = true },
                            showAppGuardPrompt = !isDeviceOwner && alwaysUseAppGuard && !isAppGuardOn,
                            onTurnOnAppGuard = { showAppGuardHelp = true },
                        )
                    } else {
                        FocusScreen(
                            remainingMillis = sessionRemainingMillis,
                            durationMinutes = sessionRoutine?.durationMinutes ?: durationMinutes,
                            onStop = viewModel::stop,
                            onPause = viewModel::pause,
                            onResume = viewModel::resume,
                            allowedApps = appsIn(sessionRoutine?.allowedApps.orEmpty(), installedApps),
                            onOpenApp = { openApp(it.packageName) },
                            tasks = sessionRoutine?.tasks.orEmpty(),
                            isPaused = isPaused,
                            canOpenApps = isDeviceOwner || isAppGuardOn,
                        )
                    }
                }

                // Coming back from the settings with the guard on is the end of the steps
                LaunchedEffect(isAppGuardOn) {
                    if (isAppGuardOn) showAppGuardHelp = false
                }
                if (showAppGuardHelp) {
                    AppGuardHelpDialog(
                        mayBeRestricted = remember { AppGuardService.mayBeRestricted(this) },
                        onOpenAppInfo = {
                            startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
                            )
                        },
                        onOpenAccessibilitySettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                        onDismiss = { showAppGuardHelp = false },
                    )
                }
                // Waits for the end of a session, which shouldn't be interrupted or left for a browser
                availableUpdateVersion?.takeIf { remainingMillis == null }?.let { newVersion ->
                    UpdateDialog(
                        newVersion = newVersion,
                        currentVersion = BuildConfig.VERSION_NAME,
                        onDownload = {
                            availableUpdateVersion = null
                            downloadUpdate()
                        },
                        onDismiss = { availableUpdateVersion = null },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isAppGuardOn = AppGuardService.isOn(this)
    }

    private fun shareApp() {
        val message = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_message, DOWNLOAD_PAGE_URL))
        startActivity(Intent.createChooser(message, getString(R.string.share_app)))
    }

    private fun downloadUpdate() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, LATEST_APK_URL.toUri()))
        } catch (_: ActivityNotFoundException) {
            // No browser on this phone, so there's nothing to hand the download to
        }
    }

    // The apps with a launcher icon, except this one, by label; icons are drawn at the size they're shown,
    // since full-size ones are slower to make and take more memory
    private fun launchableApps(): List<InstalledApp> {
        val iconSizePx = (APP_ICON_SIZE.value * resources.displayMetrics.density).roundToInt()
        return packageManager
            .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != packageName }
            .map { app ->
                InstalledApp(
                    packageName = app.packageName,
                    label = app.loadLabel(packageManager).toString(),
                    icon = BitmapPainter(
                        app.loadIcon(packageManager).toBitmap(iconSizePx, iconSizePx).asImageBitmap(),
                    ),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    override fun onStart() {
        super.onStart()
        releaseAllowedAppWakeLock()
    }

    // Harmless when the stop is the screen turning off: a screen wake lock neither wakes it nor keeps the CPU up
    override fun onStop() {
        super.onStop()
        if (isFocusActive) allowedAppWakeLock.acquire(ALLOWED_APP_WAKE_LOCK_TIMEOUT_MILLIS)
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseAllowedAppWakeLock()
    }

    private fun releaseAllowedAppWakeLock() = allowedAppWakeLock.release()

    // Opened into this task rather than a new one, since screen pinning blocks new tasks; Back returns here
    private fun openApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.removeFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun alarmSoundPickerIntent(current: Uri?) = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.alarm_sound))
        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current)

    // getTitle can fail on a sound that has since been deleted
    private fun alarmSoundTitle(sound: Uri?): String =
        if (sound == null) getString(R.string.alarm_sound_silent)
        else runCatching { RingtoneManager.getRingtone(this, sound)?.getTitle(this) }.getOrNull()
            ?: getString(R.string.alarm_sound_unknown)

    // The system bars stay visible: lock task mode already empties them, and hiding them would
    // make Android show a misleading "swipe down from the top to exit" hint
    private fun setFocusMode(active: Boolean) {
        focusModeApplied = active
        if (active) {
            focusLock.lock(viewModel.sessionRoutine.value?.allowedApps.orEmpty(), viewModel.alwaysUseAppGuard.value)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            focusLock.unlock()
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Consuming these keeps the window from falling back to adjusting the volume
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        (isFocusActive && keyCode in BLOCKED_KEYS) || super.onKeyDown(keyCode, event)

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
        (isFocusActive && keyCode in BLOCKED_KEYS) || super.onKeyUp(keyCode, event)

    private companion object {
        // The longest session; a backstop in case this process is frozen and can't release the lock itself
        const val ALLOWED_APP_WAKE_LOCK_TIMEOUT_MILLIS = 3 * 60 * 60 * 1000L

        val BLOCKED_KEYS = setOf(
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE,
        )
    }
}
