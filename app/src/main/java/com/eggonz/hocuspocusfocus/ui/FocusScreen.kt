package com.eggonz.hocuspocusfocus.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggonz.hocuspocusfocus.DEFAULT_FOCUS_DURATION_MINUTES
import com.eggonz.hocuspocusfocus.R
import com.eggonz.hocuspocusfocus.RoutineTask
import com.eggonz.hocuspocusfocus.ui.theme.DisplayFontFamily
import com.eggonz.hocuspocusfocus.ui.theme.Glow
import com.eggonz.hocuspocusfocus.ui.theme.HocusPocusFocusTheme
import com.eggonz.hocuspocusfocus.ui.theme.Night

/**
 * The countdown of a [durationMinutes] session with the [allowedApps] it can open, which are greyed out and can't be
 * tapped or scrolled unless [canOpenApps], and pause or resume (by [isPaused]) and stop buttons, stop asking for
 * confirmation first. With [tasks], the countdown shrinks to the top of the screen, above a grid of the tasks that
 * toggle on tap.
 */
@Composable
fun FocusScreen(
    remainingMillis: Long,
    durationMinutes: Int,
    onStop: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    allowedApps: List<InstalledApp>,
    onOpenApp: (InstalledApp) -> Unit,
    tasks: List<RoutineTask> = emptyList(),
    isPaused: Boolean = false,
    canOpenApps: Boolean = true,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp)
                // Centered on the content, where the countdown sits, not on the whole screen, whose system bars differ
                // in height; with tasks the countdown sits at the top and brings its own halo
                .then(if (tasks.isEmpty()) Modifier.halo(HALO_RADIUS) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            FocusContent(
                remainingMillis,
                durationMinutes,
                allowedApps,
                onOpenApp,
                tasks,
                canOpenApps,
                isPaused,
                onPause,
                onResume,
                onStop,
            )
        }
    }
}

// Fades into Night, so it can overflow the bounds of what it's drawn behind
internal fun Modifier.halo(radius: Dp) = drawBehind {
    val radiusPx = radius.toPx()
    drawCircle(Brush.radialGradient(listOf(Glow, Night), center, radiusPx), radiusPx)
}

internal val HALO_RADIUS = 180.dp
private val RING_DIAMETER = 320.dp
private val COMPACT_RING_DIAMETER = 220.dp

@Composable
private fun FocusContent(
    remainingMillis: Long,
    durationMinutes: Int,
    allowedApps: List<InstalledApp>,
    onOpenApp: (InstalledApp) -> Unit,
    tasks: List<RoutineTask>,
    canOpenApps: Boolean,
    isPaused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    var confirmStop by rememberSaveable { mutableStateOf(false) }
    if (tasks.isEmpty()) {
        RingWithAppsBelow {
            CountdownRing(remainingMillis, durationMinutes, isPaused, onPause, onResume, { confirmStop = true })
            if (allowedApps.isNotEmpty()) {
                AllowedAppsRow(allowedApps, onOpenApp, canOpenApps)
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 50.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CountdownRing(
                remainingMillis,
                durationMinutes,
                isPaused,
                onPause,
                onResume,
                { confirmStop = true },
                diameter = COMPACT_RING_DIAMETER,
                modifier = Modifier.halo(HALO_RADIUS * (COMPACT_RING_DIAMETER / RING_DIAMETER)),
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                TaskGrid(tasks)
            }
            if (allowedApps.isNotEmpty()) {
                AllowedAppsRow(allowedApps, onOpenApp, canOpenApps)
            }
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

/** The countdown inside its progress ring, up to [diameter] wide, with the pause or resume and stop buttons. */
@Composable
private fun CountdownRing(
    remainingMillis: Long,
    durationMinutes: Int,
    isPaused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    diameter: Dp = RING_DIAMETER,
    modifier: Modifier = Modifier,
) {
    val scale = diameter / RING_DIAMETER
    Box(
        modifier = modifier
            .widthIn(max = diameter)
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
                fontSize = (if (countdown.length > 5) 56.sp else 76.sp) * scale,
                fontWeight = FontWeight.Light,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                IconButton(onClick = if (isPaused) onResume else onPause) {
                    Icon(
                        painterResource(if (isPaused) R.drawable.ic_play else R.drawable.ic_pause),
                        stringResource(if (isPaused) R.string.resume else R.string.pause),
                    )
                }
                IconButton(onClick = onStop) {
                    Icon(painterResource(R.drawable.ic_stop), stringResource(R.string.stop))
                }
            }
        }
    }
}

private const val TASK_COLUMNS = 5
private val TASK_LABEL_MAX_WIDTH = 160.dp
private val TASK_LABEL_POINTER_WIDTH = 12.dp
private val TASK_LABEL_POINTER_HEIGHT = 6.dp

private val TaskLabelPointerShape = GenericShape { size, _ ->
    lineTo(size.width, 0f)
    lineTo(size.width / 2, size.height)
    close()
}

/** The [tasks] in rows of [TASK_COLUMNS], each a [TaskButton]. */
@Composable
private fun TaskGrid(tasks: List<RoutineTask>) {
    // The task whose label is showing or last showed. Not saveable and only set by a tap, so a restored toggle doesn't
    // show its label again
    var labelledTaskIndex by remember { mutableStateOf<Int?>(null) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(TASK_COLUMNS),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        // The grid clips to its bounds, so this keeps room for the first row's labels; on both sides so the grid stays
        // centered between the clock and the apps
        contentPadding = PaddingValues(vertical = 36.dp),
        modifier = Modifier
            .widthIn(max = RING_DIAMETER)
            .fillMaxWidth(),
    ) {
        itemsIndexed(tasks) { index, task ->
            // The outer columns' labels line up with the grid's edge, so long ones stay on screen
            val labelAlignment = when (index % TASK_COLUMNS) {
                0 -> Alignment.Start
                TASK_COLUMNS - 1 -> Alignment.End
                else -> Alignment.CenterHorizontally
            }
            TaskButton(
                task,
                labelAlignment,
                isLabelled = labelledTaskIndex == index,
                onToggle = { isToggled ->
                    if (isToggled) {
                        labelledTaskIndex = index
                    } else if (labelledTaskIndex == index) {
                        labelledTaskIndex = null
                    }
                },
            )
        }
    }
}

/**
 * A round button that toggles on tap, which only calls [onToggle]. While [isLabelled], the task's label shows above it
 * and then fades out, and it fades out right away once [isLabelled] turns off.
 */
@Composable
private fun TaskButton(
    task: RoutineTask,
    labelAlignment: Alignment.Horizontal,
    isLabelled: Boolean,
    onToggle: (isToggled: Boolean) -> Unit,
) {
    // Saveable, so it survives opening an allowed app, and it's forgotten when the session ends
    var isToggled by rememberSaveable { mutableStateOf(false) }
    var isLabelVisible by remember { mutableStateOf(false) }
    val labelAlpha = remember { Animatable(0f) }
    LaunchedEffect(isLabelled) {
        if (isLabelled) {
            isLabelVisible = true
            labelAlpha.snapTo(1f)
            labelAlpha.animateTo(0f, tween(durationMillis = 500, delayMillis = 600))
        } else {
            labelAlpha.animateTo(0f, tween(durationMillis = 100))
        }
        isLabelVisible = false
    }

    Box(contentAlignment = Alignment.Center) {
        FilledIconToggleButton(
            checked = isToggled,
            onCheckedChange = {
                isToggled = it
                onToggle(it)
            },
            // Like the icon dialogs' cells: only a toggled one gets a background, with the icon cut out of it
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkedContentColor = MaterialTheme.colorScheme.background,
            ),
            modifier = Modifier.size(APP_ICON_SIZE),
        ) {
            Icon(painterResource(task.icon.drawable), task.label)
        }
        if (isLabelVisible && task.label.isNotBlank()) {
            TaskLabelBubble(
                task.label,
                labelAlignment,
                alpha = { labelAlpha.value },
                modifier = Modifier.align(AbsoluteAlignment.TopLeft),
            )
        }
    }
}

/**
 * A speech bubble above its cell with [label], whose pointer touches the top of the cell's button at its center. Takes
 * no space, so it can be wider than the cell and doesn't move the button.
 */
@Composable
private fun TaskLabelBubble(
    label: String,
    alignment: Alignment.Horizontal,
    alpha: () -> Float,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.surfaceVariant
    Layout(
        content = {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .graphicsLayer { this.alpha = alpha() }
                    .background(color, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
            Box(
                Modifier
                    .size(TASK_LABEL_POINTER_WIDTH, TASK_LABEL_POINTER_HEIGHT)
                    .graphicsLayer { this.alpha = alpha() }
                    .background(color, TaskLabelPointerShape),
            )
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val cellWidth = constraints.maxWidth
        val bubble = measurables[0].measure(Constraints(maxWidth = TASK_LABEL_MAX_WIDTH.roundToPx()))
        val pointer = measurables[1].measure(Constraints())
        // One narrower than the cell is centered, or the pointer would miss its straight edge
        val bubbleX = if (bubble.width < cellWidth) {
            (cellWidth - bubble.width) / 2
        } else {
            alignment.align(bubble.width, cellWidth, layoutDirection)
        }
        layout(0, 0) {
            bubble.place(bubbleX, -pointer.height - bubble.height)
            pointer.place((cellWidth - pointer.width) / 2, -pointer.height)
        }
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

@Preview(showBackground = true)
@Composable
private fun RoutineFocusPreview() {
    HocusPocusFocusTheme {
        FocusScreen(
            remainingMillis = 60 * 60_000L,
            durationMinutes = 60,
            onStop = {},
            onPause = {},
            onResume = {},
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
            onStop = {},
            onPause = {},
            onResume = {},
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
            onStop = {},
            onPause = {},
            onResume = {},
            allowedApps = List(4) { previewApps() }.flatten(),
            onOpenApp = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TasksFocusPreview() {
    HocusPocusFocusTheme {
        FocusScreen(
            remainingMillis = 60 * 60_000L,
            durationMinutes = 60,
            onStop = {},
            onPause = {},
            onResume = {},
            allowedApps = previewApps(),
            onOpenApp = {},
            tasks = List(20) { RoutineTask(IconPool.entries[it], "Task $it") },
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
            onStop = {},
            onPause = {},
            onResume = {},
            allowedApps = emptyList(),
            onOpenApp = {},
        )
    }
}
