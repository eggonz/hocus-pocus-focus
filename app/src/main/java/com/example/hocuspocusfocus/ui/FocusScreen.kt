package com.example.hocuspocusfocus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hocuspocusfocus.DEFAULT_FOCUS_DURATION_MINUTES
import com.example.hocuspocusfocus.FOCUS_DURATIONS_MINUTES
import com.example.hocuspocusfocus.R
import com.example.hocuspocusfocus.ui.theme.DisplayFontFamily
import com.example.hocuspocusfocus.ui.theme.Glow
import com.example.hocuspocusfocus.ui.theme.HocusPocusFocusTheme
import com.example.hocuspocusfocus.ui.theme.Night

/**
 * Shows the big START button, the [durationMinutes] picker and the settings button when
 * [remainingMillis] is null, and the countdown otherwise.
 */
@Composable
fun FocusScreen(
    remainingMillis: Long?,
    durationMinutes: Int,
    onStepDuration: (steps: Int) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(Glow, Night)))
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (remainingMillis == null) {
                IdleContent(durationMinutes, onStepDuration, onStart, onOpenSettings)
            } else {
                FocusContent(remainingMillis, durationMinutes, onStop)
            }
        }
    }
}

@Composable
private fun IdleContent(
    durationMinutes: Int,
    onStepDuration: (steps: Int) -> Unit,
    onStart: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = onOpenSettings,
            // Lines the icon itself up with the content padding, not its touch target
            modifier = Modifier.align(Alignment.TopStart).offset((-12).dp, (-12).dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_settings),
                stringResource(R.string.settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = onStart,
                shape = CircleShape,
                modifier = Modifier
                    .size(240.dp)
                    .shadow(48.dp, CircleShape, ambientColor = MaterialTheme.colorScheme.primary, spotColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    stringResource(R.string.start),
                    fontFamily = DisplayFontFamily,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                )
            }
            Spacer(Modifier.height(40.dp))
            DurationPicker(durationMinutes, onStepDuration)
        }
    }
}

@Composable
private fun DurationPicker(durationMinutes: Int, onStepDuration: (steps: Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
            .padding(4.dp),
    ) {
        IconButton(
            onClick = { onStepDuration(-1) },
            enabled = durationMinutes != FOCUS_DURATIONS_MINUTES.first(),
        ) {
            Icon(painterResource(R.drawable.ic_minus), stringResource(R.string.shorter))
        }
        Text(
            text = formatDuration(durationMinutes),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
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
private fun FocusContent(remainingMillis: Long, durationMinutes: Int, onStop: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            // The picker is hidden during a session, so durationMinutes is still the session length
            CircularProgressIndicator(
                progress = { (remainingMillis / (durationMinutes * 60_000f)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 6.dp,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.focus_label).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 6.sp,
                    color = MaterialTheme.colorScheme.secondary,
                )
                val countdown = formatCountdown(remainingMillis)
                Text(
                    text = countdown,
                    // Tabular figures, so the digits don't shift as they tick
                    style = TextStyle(fontFamily = DisplayFontFamily, fontFeatureSettings = "tnum"),
                    // h:mm:ss needs a smaller size to fit inside the ring
                    fontSize = if (countdown.length > 5) 56.sp else 76.sp,
                    fontWeight = FontWeight.Light,
                )
            }
        }
        Spacer(Modifier.height(48.dp))
        OutlinedButton(onClick = onStop, modifier = Modifier.widthIn(min = 140.dp)) {
            Text(stringResource(R.string.stop), style = MaterialTheme.typography.titleMedium)
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
            onStepDuration = {},
            onStart = {},
            onStop = {},
            onOpenSettings = {},
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
            onStepDuration = {},
            onStart = {},
            onStop = {},
            onOpenSettings = {},
        )
    }
}
