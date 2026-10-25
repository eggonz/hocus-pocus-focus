package com.example.hocuspocusfocus

import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.hocuspocusfocus.ui.FocusScreen
import com.example.hocuspocusfocus.ui.SettingsScreen
import com.example.hocuspocusfocus.ui.theme.HocusPocusFocusTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: FocusViewModel by viewModels()
    private lateinit var focusLock: FocusLock

    // What was last applied to the window and lock task, so that resuming (e.g. after the screen
    // pinning prompt) doesn't lock again, and an ongoing session is re-locked after recreation
    private var focusModeApplied = false

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

        setContent {
            HocusPocusFocusTheme {
                val remainingMillis by viewModel.remainingMillis.collectAsStateWithLifecycle()
                val durationMinutes by viewModel.durationMinutes.collectAsStateWithLifecycle()
                val alarmSound by viewModel.alarmSound.collectAsStateWithLifecycle()
                var isDeviceOwner by remember { mutableStateOf(focusLock.isDeviceOwner) }
                var showSettings by rememberSaveable { mutableStateOf(false) }

                // Lock task mode still delivers Back to the app
                BackHandler(enabled = remainingMillis != null) {}

                // Settings are only reachable between sessions
                if (showSettings && remainingMillis == null) {
                    BackHandler { showSettings = false }
                    SettingsScreen(
                        alarmSoundTitle = remember(alarmSound) { alarmSoundTitle(alarmSound) },
                        isDeviceOwner = isDeviceOwner,
                        versionName = BuildConfig.VERSION_NAME,
                        onPickAlarmSound = { pickAlarmSound.launch(alarmSoundPickerIntent(alarmSound)) },
                        onRemoveDeviceOwner = {
                            focusLock.releaseDeviceOwner()
                            isDeviceOwner = false
                        },
                        onBack = { showSettings = false },
                    )
                } else {
                    FocusScreen(
                        remainingMillis = remainingMillis,
                        durationMinutes = durationMinutes,
                        onStepDuration = viewModel::stepDuration,
                        onStart = viewModel::start,
                        onStop = viewModel::stop,
                        onOpenSettings = { showSettings = true },
                    )
                }
            }
        }
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
            focusLock.lock()
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
        val BLOCKED_KEYS = setOf(
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE,
        )
    }
}
