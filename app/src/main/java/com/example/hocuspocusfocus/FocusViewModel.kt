package com.example.hocuspocusfocus

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

const val FOCUS_DURATION_MILLIS = 30 * 60 * 1000L

/**
 * The focus session timer. The end time is persisted, so a session survives the activity or
 * process being recreated, and the countdown stays correct while the screen is off.
 */
class FocusViewModel(
    app: Application,
    // Injectable so tests can drive the countdown with virtual time
    private val now: () -> Long,
) : AndroidViewModel(app) {

    // The constructor the default ViewModel factory calls
    constructor(app: Application) : this(app, System::currentTimeMillis)

    private val prefs = app.getSharedPreferences("focus", Context.MODE_PRIVATE)

    private val _remainingMillis = MutableStateFlow<Long?>(null)

    /** Time left in the session, or null when focus mode is off. */
    val remainingMillis: StateFlow<Long?> = _remainingMillis.asStateFlow()

    private var ticker: Job? = null

    init {
        val savedEnd = prefs.getLong(KEY_END_TIME, 0L)
        if (savedEnd > now()) countDownTo(savedEnd) else clear()
    }

    fun start() {
        val end = now() + FOCUS_DURATION_MILLIS
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
            clear()
        }
    }

    private fun clear() {
        prefs.edit { remove(KEY_END_TIME) }
        _remainingMillis.value = null
    }

    private companion object {
        const val KEY_END_TIME = "end_time"
    }
}
