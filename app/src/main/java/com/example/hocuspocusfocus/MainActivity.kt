package com.example.hocuspocusfocus

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.hocuspocusfocus.ui.FocusScreen
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                var isDeviceOwner by remember { mutableStateOf(focusLock.isDeviceOwner) }

                // Lock task mode still delivers Back to the app
                BackHandler(enabled = remainingMillis != null) {}

                FocusScreen(
                    remainingMillis = remainingMillis,
                    isDeviceOwner = isDeviceOwner,
                    onStart = viewModel::start,
                    onStop = viewModel::stop,
                    onRemoveDeviceOwner = {
                        focusLock.releaseDeviceOwner()
                        isDeviceOwner = false
                    },
                )
            }
        }
    }

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

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (isFocusActive && event.keyCode in BLOCKED_KEYS) return true
        return super.dispatchKeyEvent(event)
    }

    private companion object {
        val BLOCKED_KEYS = setOf(
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE,
        )
    }
}
