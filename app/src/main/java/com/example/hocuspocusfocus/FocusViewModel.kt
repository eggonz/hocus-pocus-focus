package com.example.hocuspocusfocus

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.Settings
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The session lengths the user can pick from, shortest first. */
val FOCUS_DURATIONS_MINUTES = listOf(5, 10, 15, 20, 30, 60, 90, 120, 180)
const val DEFAULT_FOCUS_DURATION_MINUTES = 30

/**
 * The focus session timer. The end time is persisted, so a session survives the activity or
 * process being recreated, and the countdown stays correct while the screen is off.
 */
class FocusViewModel(
    app: Application,
    // Injectable so tests can drive the countdown with virtual time
    private val now: () -> Long,
    // Injectable so tests can check the alarm without playing it
    private val playAlarm: (Uri) -> Unit,
) : AndroidViewModel(app) {

    // The constructor the default ViewModel factory calls
    constructor(app: Application) : this(app, System::currentTimeMillis, AlarmPlayer(app)::play)

    private val prefs = app.getSharedPreferences("focus", Context.MODE_PRIVATE)

    private val _remainingMillis = MutableStateFlow<Long?>(null)

    /** Time left in the session, or null when focus mode is off. */
    val remainingMillis: StateFlow<Long?> = _remainingMillis.asStateFlow()

    private val _durationMinutes = MutableStateFlow(
        prefs.getInt(KEY_DURATION, DEFAULT_FOCUS_DURATION_MINUTES)
            .takeIf { it in FOCUS_DURATIONS_MINUTES } ?: DEFAULT_FOCUS_DURATION_MINUTES,
    )

    /** Length of the next session, one of [FOCUS_DURATIONS_MINUTES]. Remembered across launches. */
    val durationMinutes: StateFlow<Int> = _durationMinutes.asStateFlow()

    // Saved as "" for silent; never chosen means the system's default alarm sound
    private val _alarmSound = MutableStateFlow(
        when (val saved = prefs.getString(KEY_ALARM_SOUND, null)) {
            null -> Settings.System.DEFAULT_ALARM_ALERT_URI
            "" -> null
            else -> saved.toUri()
        },
    )

    /** Sound played once when a session runs out, or null for silent. Remembered across launches. */
    val alarmSound: StateFlow<Uri?> = _alarmSound.asStateFlow()

    private var ticker: Job? = null

    init {
        val savedEnd = prefs.getLong(KEY_END_TIME, 0L)
        if (savedEnd > now()) countDownTo(savedEnd) else clear()
    }

    /** Moves the selected duration [steps] entries along [FOCUS_DURATIONS_MINUTES], stopping at the ends. */
    fun stepDuration(steps: Int) {
        val index = FOCUS_DURATIONS_MINUTES.indexOf(_durationMinutes.value) + steps
        val minutes = FOCUS_DURATIONS_MINUTES[index.coerceIn(FOCUS_DURATIONS_MINUTES.indices)]
        prefs.edit { putInt(KEY_DURATION, minutes) }
        _durationMinutes.value = minutes
    }

    fun setAlarmSound(sound: Uri?) {
        prefs.edit { putString(KEY_ALARM_SOUND, sound?.toString().orEmpty()) }
        _alarmSound.value = sound
    }

    fun start() {
        val end = now() + _durationMinutes.value * 60_000L
        prefs.edit { putLong(KEY_END_TIME, end) }
        countDownTo(end)
    }

    fun stop() {
        ticker?.cancel()
        clear()
    }

    private fun countDownTo(end: Long) {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                val left = end - now()
                if (left <= 0) break
                _remainingMillis.value = left
                // Wake up exactly when the displayed second changes
                delay((left - 1) % 1000 + 1)
            }
            // Only a session that runs out rings, not one that is stopped
            _alarmSound.value?.let(playAlarm)
            clear()
        }
    }

    private fun clear() {
        prefs.edit { remove(KEY_END_TIME) }
        _remainingMillis.value = null
    }

    private companion object {
        const val KEY_END_TIME = "end_time"
        const val KEY_DURATION = "duration_minutes"
        const val KEY_ALARM_SOUND = "alarm_sound"
    }
}
