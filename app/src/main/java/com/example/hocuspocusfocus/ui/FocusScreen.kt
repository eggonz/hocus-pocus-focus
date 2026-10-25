package com.example.hocuspocusfocus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hocuspocusfocus.DEFAULT_FOCUS_DURATION_MINUTES
import com.example.hocuspocusfocus.FOCUS_DURATIONS_MINUTES
import com.example.hocuspocusfocus.R
import com.example.hocuspocusfocus.ui.theme.HocusPocusFocusTheme

/**
 * Shows the big START button and the [durationMinutes] picker when [remainingMillis] is null, and
 * the countdown otherwise.
 */
@Composable
fun FocusScreen(
    remainingMillis: Long?,
    durationMinutes: Int,
    isDeviceOwner: Boolean,
    onStepDuration: (steps: Int) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRemoveDeviceOwner: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (remainingMillis == null) {
                IdleContent(durationMinutes, isDeviceOwner, onStepDuration, onStart, onRemoveDeviceOwner)
            } else {
                FocusContent(remainingMillis, onStop)
            }
        }
    }
}

@Composable
private fun IdleContent(
    durationMinutes: Int,
    isDeviceOwner: Boolean,
    onStepDuration: (steps: Int) -> Unit,
    onStart: () -> Unit,
    onRemoveDeviceOwner: () -> Unit,
) {
    var confirmRemoval by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = onStart, shape = CircleShape, modifier = Modifier.size(260.dp)) {
                Text(stringResource(R.string.start), fontSize = 44.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
            DurationPicker(durationMinutes, onStepDuration)
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isDeviceOwner) {
                TextButton(onClick = { confirmRemoval = true }) {
                    Text(stringResource(R.string.remove_device_owner))
                }
            } else {
                Text(
                    text = stringResource(R.string.soft_lock_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (confirmRemoval) {
        AlertDialog(
            onDismissRequest = { confirmRemoval = false },
            title = { Text(stringResource(R.string.remove_device_owner)) },
            text = { Text(stringResource(R.string.remove_device_owner_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemoval = false
                    onRemoveDeviceOwner()
                }) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemoval = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun DurationPicker(durationMinutes: Int, onStepDuration: (steps: Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onStepDuration(-1) },
            enabled = durationMinutes != FOCUS_DURATIONS_MINUTES.first(),
        ) {
            Icon(painterResource(R.drawable.ic_minus), stringResource(R.string.shorter))
        }
        Text(
            text = formatDuration(durationMinutes),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            // Wide enough for the longest label, so the buttons don't move as it changes
            modifier = Modifier.widthIn(min = 112.dp),
        )
        IconButton(
            onClick = { onStepDuration(1) },
            enabled = durationMinutes != FOCUS_DURATIONS_MINUTES.last(),
        ) {
            Icon(painterResource(R.drawable.ic_plus), stringResource(R.string.longer))
        }
    }
}

@Composable
private fun FocusContent(remainingMillis: Long, onStop: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.focus_label),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val countdown = formatCountdown(remainingMillis)
        Text(
            text = countdown,
            // h:mm:ss needs a smaller size to fit the screen width
            fontSize = if (countdown.length > 5) 72.sp else 96.sp,
            fontWeight = FontWeight.Light,
        )
        Spacer(Modifier.height(48.dp))
        OutlinedButton(onClick = onStop) {
            Text(stringResource(R.string.stop))
        }
    }
}

/**
 * Formats as mm:ss, or h:mm:ss from an hour up, rounding up so the display reads 30:00 at the
 * start and 00:01 last.
 */
internal fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis + 999) / 1000
    val hours = totalSeconds / 3600
    val minutes = totalSeconds / 60 % 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}

/** Formats a picker duration as 5 min, 1 h or 1.5 h. */
internal fun formatDuration(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60.0} h"
}

@Preview(showBackground = true)
@Composable
private fun IdlePreview() {
    HocusPocusFocusTheme {
        FocusScreen(
            remainingMillis = null,
            durationMinutes = DEFAULT_FOCUS_DURATION_MINUTES,
            isDeviceOwner = false,
            onStepDuration = {},
            onStart = {},
            onStop = {},
            onRemoveDeviceOwner = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FocusPreview() {
    HocusPocusFocusTheme {
        FocusScreen(
            remainingMillis = DEFAULT_FOCUS_DURATION_MINUTES * 60_000L,
            durationMinutes = DEFAULT_FOCUS_DURATION_MINUTES,
            isDeviceOwner = true,
            onStepDuration = {},
            onStart = {},
            onStop = {},
            onRemoveDeviceOwner = {},
        )
    }
}
