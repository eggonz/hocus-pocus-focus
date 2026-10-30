package com.eggonz.hocuspocusfocus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggonz.hocuspocusfocus.DEFAULT_FOCUS_DURATION_MINUTES
import com.eggonz.hocuspocusfocus.FOCUS_DURATIONS_MINUTES
import com.eggonz.hocuspocusfocus.R
import com.eggonz.hocuspocusfocus.ui.theme.DisplayFontFamily
import com.eggonz.hocuspocusfocus.ui.theme.Glow
import com.eggonz.hocuspocusfocus.ui.theme.HocusPocusFocusTheme

/**
 * The big START button, the [durationMinutes] picker and the settings and routines buttons, or, when
 * [showAppGuardPrompt], a prompt to turn on the app guard instead of all but the settings button.
 */
@Composable
fun HomeScreen(
    durationMinutes: Int,
    onStepDuration: (steps: Int) -> Unit,
    onStart: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRoutines: () -> Unit,
    showAppGuardPrompt: Boolean = false,
    onTurnOnAppGuard: () -> Unit = {},
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp)
                // Centered on the content, where START sits, not on the whole screen, whose system bars differ
                // in height
                .halo(HALO_RADIUS),
        ) {
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

            // The prompt takes START's place, and the rest keeps its space so the prompt lines up with START
            val hiddenForPrompt = if (showAppGuardPrompt) Modifier.unplaced() else Modifier
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RoutinesButton(onOpenRoutines, hiddenForPrompt)
                if (showAppGuardPrompt) {
                    Box(modifier = Modifier.height(START_BUTTON_DIAMETER), contentAlignment = Alignment.Center) {
                        AppGuardPrompt(onTurnOnAppGuard)
                    }
                } else {
                    StartButton(onStart)
                }
                DurationPicker(durationMinutes, onStepDuration, hiddenForPrompt)
            }
        }
    }
}

/** A pill the size of the [DurationPicker], so the controls above and below START match. */
@Composable
private fun RoutinesButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        // Not the dimmer default onSurfaceVariant, so the label matches the picker's duration
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            // Same slots as the picker's buttons, so the label lines up with its duration
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                // The label already names the button
                Icon(painterResource(R.drawable.ic_routines), contentDescription = null)
            }
            Text(
                stringResource(R.string.routines),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = PILL_LABEL_MIN_WIDTH),
            )
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                )
            }
        }
    }
}

// Measured but never placed, so it keeps its space without being drawn, tapped or read out
private fun Modifier.unplaced() = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {}
}

@Composable
private fun AppGuardPrompt(onTurnOnAppGuard: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = 320.dp),
    ) {
        Text(
            stringResource(R.string.app_guard_required),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onTurnOnAppGuard) { Text(stringResource(R.string.turn_on)) }
    }
}

private val START_BUTTON_DIAMETER = 240.dp

// Wide enough for the longest duration, and shared so the routines button matches the picker
private val PILL_LABEL_MIN_WIDTH = 144.dp

/** The glowing round START button, with its glow and label scaled to its [diameter]. */
@Composable
internal fun StartButton(onClick: () -> Unit, diameter: Dp = START_BUTTON_DIAMETER) {
    val scale = diameter / START_BUTTON_DIAMETER
    Button(
        onClick = onClick,
        shape = CircleShape,
        // The label is sized to fit the circle, so the default side padding would only make it wrap
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .size(diameter)
            .shadow(
                48.dp * scale,
                CircleShape,
                ambientColor = Glow,
                spotColor = Glow,
            ),
    ) {
        Text(
            stringResource(R.string.start),
            fontFamily = DisplayFontFamily,
            fontSize = 44.sp * scale,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp * scale,
            maxLines = 1,
        )
    }
}

@Composable
private fun DurationPicker(
    durationMinutes: Int,
    onStepDuration: (steps: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
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
            modifier = Modifier.widthIn(min = PILL_LABEL_MIN_WIDTH),
        )
        IconButton(
            onClick = { onStepDuration(1) },
            enabled = durationMinutes != FOCUS_DURATIONS_MINUTES.last(),
        ) {
            Icon(painterResource(R.drawable.ic_plus), stringResource(R.string.longer))
        }
    }
}

/** Formats a picker duration as 5 min, 1 h or 1 h 35 min. */
internal fun formatDuration(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    HocusPocusFocusTheme {
        HomeScreen(
            durationMinutes = DEFAULT_FOCUS_DURATION_MINUTES,
            onStepDuration = {},
            onStart = {},
            onOpenSettings = {},
            onOpenRoutines = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppGuardPromptPreview() {
    HocusPocusFocusTheme {
        HomeScreen(
            durationMinutes = DEFAULT_FOCUS_DURATION_MINUTES,
            onStepDuration = {},
            onStart = {},
            onOpenSettings = {},
            onOpenRoutines = {},
            showAppGuardPrompt = true,
        )
    }
}
