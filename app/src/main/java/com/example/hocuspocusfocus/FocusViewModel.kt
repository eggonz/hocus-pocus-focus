package com.example.hocuspocusfocus

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.Settings
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hocuspocusfocus.ui.IconPool
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The session lengths the user can pick from, in minutes, shortest first. */
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

    private val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _remainingMillis = MutableStateFlow<Long?>(null)

    /** Time left in the session, or null when focus mode is off. */
    val remainingMillis: StateFlow<Long?> = _remainingMillis.asStateFlow()

    // Falls back to the duration saved in seconds by v0.2.0
    private val _durationMinutes = MutableStateFlow(
        prefs.getInt(KEY_DURATION, prefs.getInt(KEY_DURATION_SECONDS, 0) / 60)
            .takeIf { it in FOCUS_DURATIONS_MINUTES } ?: DEFAULT_FOCUS_DURATION_MINUTES,
    )

    /** Length of the next session in minutes, one of [FOCUS_DURATIONS_MINUTES]. Remembered across launches. */
    val durationMinutes: StateFlow<Int> = _durationMinutes.asStateFlow()

    // Saved as "" for silent; never chosen means the system's default notification sound, which is short
    private val _alarmSound = MutableStateFlow(
        when (val saved = prefs.getString(KEY_ALARM_SOUND, null)) {
            null -> Settings.System.DEFAULT_NOTIFICATION_URI
            "" -> null
            else -> saved.toUri()
        },
    )

    /** Sound played once when a session runs out, or null for silent. Remembered across launches. */
    val alarmSound: StateFlow<Uri?> = _alarmSound.asStateFlow()

    private val _alwaysUseAppGuard = MutableStateFlow(prefs.getBoolean(KEY_ALWAYS_USE_APP_GUARD, false))

    /** Whether sessions without allowed apps use the app guard too, not pinning. Remembered across launches. */
    val alwaysUseAppGuard: StateFlow<Boolean> = _alwaysUseAppGuard.asStateFlow()

    // Not in the companion object because its text comes from string resources, which need the app
    private val SAMPLE_ROUTINE = Routine(
        id = 0,
        IconPool.DEFAULT,
        app.getString(R.string.routine_morning),
        60,
        tasks = listOf(
            RoutineTask(IconPool.COFFEE, app.getString(R.string.routine_morning_coffee)),
            RoutineTask(IconPool.BREAKFAST, app.getString(R.string.routine_morning_breakfast)),
            RoutineTask(IconPool.BED, app.getString(R.string.routine_morning_bed)),
            RoutineTask(IconPool.GLASS_OF_WATER, app.getString(R.string.routine_morning_water)),
            RoutineTask(IconPool.SHOWER, app.getString(R.string.routine_morning_shower)),
            RoutineTask(IconPool.TOOTHBRUSH, app.getString(R.string.routine_morning_teeth)),
            RoutineTask(IconPool.TSHIRT, app.getString(R.string.routine_morning_clothes)),
            RoutineTask(IconPool.MEDITATION, app.getString(R.string.routine_morning_meditate)),
            RoutineTask(IconPool.STRETCHING, app.getString(R.string.routine_morning_stretching)),
        ),
    )

    // Until the user edits one, the routines are the sample routine
    private val _routines = MutableStateFlow(
        prefs.getString(KEY_ROUTINES, null)?.let(::routinesFromJson) ?: listOf(SAMPLE_ROUTINE),
    )

    /** The user's routines. Remembered across launches. */
    val routines: StateFlow<List<Routine>> = _routines.asStateFlow()

    private val _sessionRoutine = MutableStateFlow<Routine?>(null)

    /** The routine the running session was started from, or null for a plain session or none. */
    val sessionRoutine: StateFlow<Routine?> = _sessionRoutine.asStateFlow()

    private val _isPaused = MutableStateFlow(false)

    /** Whether the running session's countdown is paused; the phone stays locked meanwhile. */
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private var ticker: Job? = null

    init {
        val savedEnd = prefs.getLong(KEY_END_TIME, 0L)
        val pausedRemaining = prefs.getLong(KEY_PAUSED_REMAINING, 0L)
        val savedRoutineId = prefs.getInt(KEY_ROUTINE_ID, NO_ROUTINE)
        if (pausedRemaining > 0) {
            _sessionRoutine.value = _routines.value.find { it.id == savedRoutineId }
            _remainingMillis.value = pausedRemaining
            _isPaused.value = true
        } else if (savedEnd > now()) {
            _sessionRoutine.value = _routines.value.find { it.id == savedRoutineId }
            countDownTo(savedEnd)
        } else {
            clear()
        }
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

    fun setAlwaysUseAppGuard(isOn: Boolean) {
        prefs.edit { putBoolean(KEY_ALWAYS_USE_APP_GUARD, isOn) }
        _alwaysUseAppGuard.value = isOn
    }

    /** Replaces the routine with the same id as [routine]. */
    fun updateRoutine(routine: Routine) {
        val routines = _routines.value.map { if (it.id == routine.id) routine else it }
        prefs.edit { putString(KEY_ROUTINES, routines.toJson()) }
        _routines.value = routines
    }

    /** Adds a one hour routine with no title and no allowed apps, and returns it. */
    fun addRoutine(): Routine {
        val routine = Routine(
            id = (_routines.value.maxOfOrNull { it.id } ?: -1) + 1,
            IconPool.DEFAULT,
            title = "",
            durationMinutes = 60,
        )
        val routines = _routines.value + routine
        prefs.edit { putString(KEY_ROUTINES, routines.toJson()) }
        _routines.value = routines
        return routine
    }

    /** Removes the routine with the given [id]. */
    fun deleteRoutine(id: Int) {
        val routines = _routines.value.filterNot { it.id == id }
        prefs.edit { putString(KEY_ROUTINES, routines.toJson()) }
        _routines.value = routines
    }

    fun start() = startSession(_durationMinutes.value, routine = null)

    /** Starts a session as long as [routine], which keeps it around as [sessionRoutine] for its allowed apps. */
    fun startRoutine(routine: Routine) = startSession(routine.durationMinutes, routine)

    // The routine is set before the countdown, so it's there when the lock reacts to the session starting
    private fun startSession(minutes: Int, routine: Routine?) {
        val end = now() + minutes * 60_000L
        prefs.edit {
            putLong(KEY_END_TIME, end)
            putInt(KEY_ROUTINE_ID, routine?.id ?: NO_ROUTINE)
        }
        _sessionRoutine.value = routine
        countDownTo(end)
    }

    /** Freezes the countdown of the running session, which stays on until [resume]d or stopped. */
    fun pause() {
        val remaining = _remainingMillis.value ?: return
        ticker?.cancel()
        // The time left is saved instead of the end time, so a paused session survives process death too
        prefs.edit {
            putLong(KEY_PAUSED_REMAINING, remaining)
            remove(KEY_END_TIME)
        }
        _isPaused.value = true
    }

    /** Restarts the countdown of a [pause]d session from where it stopped. */
    fun resume() {
        if (!_isPaused.value) return
        val end = now() + prefs.getLong(KEY_PAUSED_REMAINING, 0L)
        prefs.edit {
            putLong(KEY_END_TIME, end)
            remove(KEY_PAUSED_REMAINING)
        }
        _isPaused.value = false
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
        prefs.edit {
            remove(KEY_END_TIME)
            remove(KEY_PAUSED_REMAINING)
            remove(KEY_ROUTINE_ID)
        }
        _remainingMillis.value = null
        _isPaused.value = false
        _sessionRoutine.value = null
    }

    internal companion object {
        private const val PREFS_NAME = "focus"
        private const val KEY_END_TIME = "end_time"
        private const val KEY_PAUSED_REMAINING = "paused_remaining"
        private const val KEY_DURATION = "duration_minutes"
        private const val KEY_DURATION_SECONDS = "duration_seconds"
        private const val KEY_ALARM_SOUND = "alarm_sound"
        private const val KEY_ROUTINES = "routines"
        private const val KEY_ALWAYS_USE_APP_GUARD = "always_use_app_guard"
        private const val KEY_ROUTINE_ID = "routine_id"
        private const val NO_ROUTINE = -1

        /**
         * The apps the saved session allows besides this one, or null when no session is running at [now]. Read
         * from what a [FocusViewModel] saved, for the app guard, which runs without one.
         */
        fun savedSessionAllowedApps(context: Context, now: Long): List<String>? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isRunning = prefs.getLong(KEY_END_TIME, 0L) > now || prefs.getLong(KEY_PAUSED_REMAINING, 0L) > 0
            if (!isRunning) return null
            val routineId = prefs.getInt(KEY_ROUTINE_ID, NO_ROUTINE)
            val routine = prefs.getString(KEY_ROUTINES, null)?.let(::routinesFromJson)?.find { it.id == routineId }
            return routine?.allowedApps.orEmpty()
        }
    }
}
