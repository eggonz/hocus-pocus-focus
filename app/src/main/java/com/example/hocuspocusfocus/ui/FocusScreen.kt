package com.example.hocuspocusfocus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hocuspocusfocus.FOCUS_DURATION_MILLIS
import com.example.hocuspocusfocus.R
import com.example.hocuspocusfocus.ui.theme.HocusPocusFocusTheme

/** Shows the big START button when [remainingMillis] is null, and the countdown otherwise. */
@Composable
fun FocusScreen(
    remainingMillis: Long?,
    isDeviceOwner: Boolean,
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
                IdleContent(isDeviceOwner, onStart, onRemoveDeviceOwner)
            } else {
                FocusContent(remainingMillis, onStop)
            }
        }
    }
}

@Composable
private fun IdleContent(
    isDeviceOwner: Boolean,
    onStart: () -> Unit,
    onRemoveDeviceOwner: () -> Unit,
) {
    var confirmRemoval by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onStart,
            shape = CircleShape,
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.Center),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.start), fontSize = 44.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.start_duration), style = MaterialTheme.typography.titleMedium)
            }
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
        Text(
            text = formatCountdown(remainingMillis),
            fontSize = 96.sp,
            fontWeight = FontWeight.Light,
        )
        Spacer(Modifier.height(48.dp))
        OutlinedButton(onClick = onStop) {
            Text(stringResource(R.string.stop))
        }
    }
}

/** Formats as mm:ss, rounding up so the display reads 30:00 at the start and 00:01 last. */
private fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis + 999) / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Preview(showBackground = true)
@Composable
private fun IdlePreview() {
    HocusPocusFocusTheme {
        FocusScreen(null, isDeviceOwner = false, onStart = {}, onStop = {}, onRemoveDeviceOwner = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun FocusPreview() {
    HocusPocusFocusTheme {
        FocusScreen(FOCUS_DURATION_MILLIS, isDeviceOwner = true, onStart = {}, onStop = {}, onRemoveDeviceOwner = {})
    }
}
