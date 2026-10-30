package com.example.hocuspocusfocus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
 * Shows the big START button, the [durationMinutes] picker and the settings and routines buttons
 * when [remainingMillis] is null (or, when [showAppGuardPrompt], a prompt to turn on the app guard
 * instead of all but the settings button), and otherwise the countdown of a [durationMinutes]
 * session with the [allowedApps] it can open, which are greyed out and can't be tapped or scrolled unless
 * [canOpenApps], and pause or resume (by [isPaused]) and stop buttons, stop asking for confirmation first.
 */
@Composable
fun FocusScreen(
    remainingMillis: Long?,
    durationMinutes: Int,
    onStepDuration: (steps: Int) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRoutines: () -> Unit,
    allowedApps: List<InstalledApp>,
    onOpenApp: (InstalledApp) -> Unit,
    isPaused: Boolean = false,
    canOpenApps: Boolean = true,
    showAppGuardPrompt: Boolean = false,
    onTurnOnAppGuard: () -> Unit = {},
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp)
                // Centered on the content, where START and the countdown sit, not on the whole screen, whose
                // system bars differ in height; it fades into Night, so it can overflow the padding
                .drawBehind {
                    val radius = 180.dp.toPx()
                    drawCircle(Brush.radialGradient(listOf(Glow, Night), center, radius), radius)
                },
            contentAlignment = Alignment.Center,
        ) {
            if (remainingMillis == null) {
                IdleContent(
                    durationMinutes,
                    onStepDuration,
                    onStart,
                    onOpenSettings,
                    onOpenRoutines,
                    showAppGuardPrompt,
                    onTurnOnAppGuard,
                )
            } else {
                FocusContent(
                    remainingMillis,
                    durationMinutes,
                    allowedApps,
                    onOpenApp,
                    canOpenApps,
                    isPaused,
                    onPause,
                    onResume,
                    onStop,
                )
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
    onOpenRoutines: () -> Unit,
    showAppGuardPrompt: Boolean,
    onTurnOnAppGuard: () -> Unit,
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

@Composable
private fun FocusContent(
    remainingMillis: Long,
    durationMinutes: Int,
    allowedApps: List<InstalledApp>,
    onOpenApp: (InstalledApp) -> Unit,
    canOpenApps: Boolean,
    isPaused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    var confirmStop by rememberSaveable { mutableStateOf(false) }
    RingWithAppsBelow {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    IconButton(onClick = if (isPaused) onResume else onPause) {
                        Icon(
                            painterResource(if (isPaused) R.drawable.ic_play else R.drawable.ic_pause),
                            stringResource(if (isPaused) R.string.resume else R.string.pause),
                        )
                    }
                    IconButton(onClick = { confirmStop = true }) {
                        Icon(painterResource(R.drawable.ic_stop), stringResource(R.string.stop))
                    }
                }
            }
        }
        if (allowedApps.isNotEmpty()) {
            AllowedAppsRow(allowedApps, onOpenApp, canOpenApps)
        }
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text(stringResource(R.string.stop_session)) },
            text = { Text(stringResource(R.string.stop_session_message)) },
            // Both in one full-width row, since the dialog would otherwise put Cancel at the end next to Stop
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { confirmStop = false }) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = {
                        confirmStop = false
                        onStop()
                    }) { Text(stringResource(R.string.stop)) }
                }
            },
        )
    }
}

/**
 * Centers the first child, the countdown ring, on the whole screen whether or not there is a second, the apps row,
 * which hangs below it; the ring only moves up when the screen is too short for the row to fit under it.
 */
@Composable
private fun RingWithAppsBelow(content: @Composable () -> Unit) {
    Layout(content, Modifier.fillMaxSize()) { measurables, constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val ring = measurables[0].measure(looseConstraints)
        val apps = measurables.getOrNull(1)?.measure(looseConstraints)
        val gap = 32.dp.roundToPx()
        val below = apps?.let { gap + it.height } ?: 0
        val ringTop = minOf((constraints.maxHeight - ring.height) / 2, constraints.maxHeight - ring.height - below)
            .coerceAtLeast(0)
        layout(constraints.maxWidth, constraints.maxHeight) {
            ring.placeRelative((constraints.maxWidth - ring.width) / 2, ringTop)
            apps?.placeRelative((constraints.maxWidth - apps.width) / 2, ringTop + ring.height + gap)
        }
    }
}

/**
 * The [allowedApps] in one line that scrolls sideways, fading out at the sides where apps are hidden, or greyed out
 * and still unless [canOpenApps].
 */
@Composable
private fun AllowedAppsRow(allowedApps: List<InstalledApp>, onOpenApp: (InstalledApp) -> Unit, canOpenApps: Boolean) {
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        userScrollEnabled = canOpenApps,
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        modifier = Modifier
            .widthIn(max = 290.dp)
            .fillMaxWidth()
            // Offscreen, so DstIn masks the icons alone and not the background behind them
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                val fadeWidth = APP_ICON_SIZE.toPx()
                if (listState.canScrollBackward) {
                    drawRect(
                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Black), endX = fadeWidth),
                        blendMode = BlendMode.DstIn,
                    )
                }
                if (listState.canScrollForward) {
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.Black, Color.Transparent),
                            startX = size.width - fadeWidth,
                            endX = size.width,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }
            },
    ) {
        items(allowedApps) { app ->
            AppIcon(app, Modifier.clickable(enabled = canOpenApps) { onOpenApp(app) }, isEnabled = canOpenApps)
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

/** Formats a picker duration as 5 min, 1 h or 1 h 35 min. */
internal fun formatDuration(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
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
            onPause = {},
            onResume = {},
            onOpenSettings = {},
            onOpenRoutines = {},
            allowedApps = emptyList(),
            onOpenApp = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppGuardPromptPreview() {
    HocusPocusFocusTheme {
        FocusScreen(
            remainingMillis = null,
            durationMinutes = DEFAULT_FOCUS_DURATION_MINUTES,
            onStepDuration = {},
            onStart = {},
            onStop = {},
            onPause = {},
            onResume = {},
            onOpenSettings = {},
            onOpenRoutines = {},
            allowedApps = emptyList(),
            onOpenApp = {},
            showAppGuardPrompt = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RoutineFocusPreview() {
    HocusPocusFocusTheme {
        FocusScreen(
            remainingMillis = 60 * 60_000L,
            durationMinutes = 60,
            onStepDuration = {},
            onStart = {},
            onStop = {},
            onPause = {},
            onResume = {},
            onOpenSettings = {},
            onOpenRoutines = {},
            allowedApps = previewApps(),
            onOpenApp = {},
            isPaused = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppsBlockedFocusPreview() {
    HocusPocusFocusTheme {
        FocusScreen(
            remainingMillis = 60 * 60_000L,
            durationMinutes = 60,
            onStepDuration = {},
            onStart = {},
            onStop = {},
            onPause = {},
            onResume = {},
            onOpenSettings = {},
            onOpenRoutines = {},
            allowedApps = List(4) { previewApps() }.flatten(),
            onOpenApp = {},
            canOpenApps = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ManyAppsFocusPreview() {
    HocusPocusFocusTheme {
        FocusScreen(
            remainingMillis = 60 * 60_000L,
            durationMinutes = 60,
            onStepDuration = {},
            onStart = {},
            onStop = {},
            onPause = {},
            onResume = {},
            onOpenSettings = {},
            onOpenRoutines = {},
            allowedApps = List(4) { previewApps() }.flatten(),
            onOpenApp = {},
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
            onPause = {},
            onResume = {},
            onOpenSettings = {},
            onOpenRoutines = {},
            allowedApps = emptyList(),
            onOpenApp = {},
        )
    }
}
