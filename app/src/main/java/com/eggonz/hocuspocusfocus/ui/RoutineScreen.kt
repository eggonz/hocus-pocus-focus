package com.eggonz.hocuspocusfocus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggonz.hocuspocusfocus.R
import com.eggonz.hocuspocusfocus.Routine
import com.eggonz.hocuspocusfocus.RoutineTask
import com.eggonz.hocuspocusfocus.ui.theme.DisplayFontFamily
import com.eggonz.hocuspocusfocus.ui.theme.Glow
import com.eggonz.hocuspocusfocus.ui.theme.HocusPocusFocusTheme
import com.eggonz.hocuspocusfocus.ui.theme.Night

private val START_BUTTON_DIAMETER_ON_ROUTINE = 113.dp

/**
 * One routine's page: its icon, title, duration, [allowedAppCount] and number of tasks, with a pencil button to edit
 * them and a START button to run it. When [showAppGuardHint], the allowed apps are marked as blocked and START is a plain
 * button under a hint to turn on the app guard.
 */
@Composable
fun RoutineScreen(
    routine: Routine,
    allowedAppCount: Int,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    showAppGuardHint: Boolean,
    onTurnOnAppGuard: () -> Unit,
    onBack: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(Night)
                .safeDrawingPadding(),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        painterResource(R.drawable.ic_edit),
                        stringResource(R.string.edit_routine),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Equal weights keep the allowed apps evenly spaced between the routine's details and the button
            Spacer(modifier = Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    // Centers the glow on the icon and title; it fades into Night, so it can overflow their bounds
                    modifier = Modifier.drawBehind {
                        val radius = 180.dp.toPx()
                        drawCircle(Brush.radialGradient(listOf(Glow, Night), center, radius), radius)
                    },
                ) {
                    // The title below already names the routine
                    Icon(
                        painterResource(routine.icon.drawable),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(96.dp),
                    )
                    Text(
                        routine.displayTitle(),
                        style = MaterialTheme.typography.displaySmall,
                        fontFamily = DisplayFontFamily,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
                Text(
                    formatDuration(routine.durationMinutes),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    pluralStringResource(R.plurals.task_count, routine.tasks.size, routine.tasks.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (allowedAppCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            pluralStringResource(R.plurals.apps_allowed, allowedAppCount, allowedAppCount),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (showAppGuardHint) {
                            Icon(
                                painterResource(R.drawable.ic_block),
                                stringResource(R.string.apps_blocked),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            // Keeps the circle's height for the plain button too, so the layout above stays put
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .padding(bottom = 48.dp)
                    .height(START_BUTTON_DIAMETER_ON_ROUTINE),
            ) {
                // Without the app guard the allowed apps are blocked, so START is toned down from the big circle
                if (showAppGuardHint) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        // Lets the hint grow up into the gap above instead of pushing the layout up
                        modifier = Modifier.wrapContentHeight(Alignment.Bottom, unbounded = true),
                    ) {
                        Text(
                            stringResource(R.string.app_guard_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                        TextButton(onClick = onTurnOnAppGuard) { Text(stringResource(R.string.turn_on)) }
                        Button(onClick = onStart, modifier = Modifier.padding(top = 10.dp)) {
                            // Same lettering as the circle's START
                            Text(
                                stringResource(R.string.start),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                            )
                        }
                    }
                } else {
                    StartButton(onStart, diameter = START_BUTTON_DIAMETER_ON_ROUTINE)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RoutinePreview() {
    HocusPocusFocusTheme {
        val apps = previewApps()
        RoutineScreen(
            Routine(
                0,
                IconPool.SUN,
                "Morning",
                60,
                apps.map { it.packageName },
                listOf(RoutineTask(IconPool.PILLS, "Vitamins")),
            ),
            allowedAppCount = apps.size,
            onEdit = {},
            onStart = {},
            showAppGuardHint = false,
            onTurnOnAppGuard = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RoutineAppGuardHintPreview() {
    HocusPocusFocusTheme {
        val apps = previewApps()
        RoutineScreen(
            Routine(0, IconPool.SUN, "Morning", 60, apps.map { it.packageName }),
            allowedAppCount = apps.size,
            onEdit = {},
            onStart = {},
            showAppGuardHint = true,
            onTurnOnAppGuard = {},
            onBack = {},
        )
    }
}
