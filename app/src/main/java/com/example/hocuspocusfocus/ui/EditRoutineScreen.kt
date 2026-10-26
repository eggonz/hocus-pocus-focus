package com.example.hocuspocusfocus.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hocuspocusfocus.FOCUS_DURATIONS_MINUTES
import com.example.hocuspocusfocus.R
import com.example.hocuspocusfocus.Routine
import com.example.hocuspocusfocus.RoutineIcon
import com.example.hocuspocusfocus.ui.theme.DisplayFontFamily
import com.example.hocuspocusfocus.ui.theme.HocusPocusFocusTheme
import kotlin.math.abs

/**
 * Edits a routine's icon, title, duration and allowed apps (picked from [installedApps]); every change is passed
 * straight to [onChange]. The trash button at the top right calls [onDelete] once confirmed.
 */
@Composable
fun EditRoutineScreen(
    routine: Routine,
    installedApps: List<InstalledApp>,
    onChange: (Routine) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    var isAddingApp by rememberSaveable { mutableStateOf(false) }
    var confirmDeletion by rememberSaveable { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.edit_routine),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = DisplayFontFamily,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { confirmDeletion = true }) {
                    Icon(
                        painterResource(R.drawable.ic_delete),
                        stringResource(R.string.delete_routine),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                OutlinedTextField(
                    value = routine.title,
                    onValueChange = { onChange(routine.copy(title = it)) },
                    label = { Text(stringResource(R.string.routine_title)) },
                    placeholder = { Text(stringResource(R.string.new_routine)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))

                Text(stringResource(R.string.routine_icon), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoutineIcon.entries.forEach { icon ->
                        FilledIconToggleButton(
                            checked = icon == routine.icon,
                            onCheckedChange = { onChange(routine.copy(icon = icon)) },
                        ) {
                            Icon(painterResource(icon.drawable), stringResource(icon.label))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                Text(stringResource(R.string.routine_duration), style = MaterialTheme.typography.titleMedium)
                PreciseDurationPicker(
                    durationMinutes = routine.durationMinutes,
                    onStepMinutes = { minutes ->
                        onChange(routine.copy(durationMinutes = stepRoutineDuration(routine.durationMinutes, minutes)))
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(16.dp))

                Text(stringResource(R.string.allowed_apps), style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    appsIn(routine.allowedApps, installedApps).forEach { app ->
                        RemovableAppIcon(
                            app,
                            onRemove = { onChange(routine.copy(allowedApps = routine.allowedApps - app.packageName)) },
                        )
                    }
                    FilledTonalIconButton(
                        onClick = { isAddingApp = true },
                        // In the middle of a removable icon's slot, so it lines up with the icons
                        modifier = Modifier.padding(BADGE_SIZE / 2).size(APP_ICON_SIZE),
                    ) {
                        Icon(painterResource(R.drawable.ic_plus), stringResource(R.string.add_app))
                    }
                }
            }
        }
    }

    if (confirmDeletion) {
        AlertDialog(
            onDismissRequest = { confirmDeletion = false },
            title = { Text(stringResource(R.string.delete_routine)) },
            text = { Text(stringResource(R.string.delete_routine_message, routine.title)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeletion = false
                    onDelete()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeletion = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (isAddingApp) {
        AddAppDialog(
            apps = installedApps.filter { it.packageName !in routine.allowedApps },
            onAdd = { app ->
                isAddingApp = false
                onChange(routine.copy(allowedApps = routine.allowedApps + app.packageName))
            },
            onDismiss = { isAddingApp = false },
        )
    }
}

private val BADGE_SIZE = 24.dp

/** An app icon with a red minus badge at its top right that calls [onRemove]. */
@Composable
private fun RemovableAppIcon(app: InstalledApp, onRemove: () -> Unit) {
    Box {
        AppIcon(app, modifier = Modifier.padding(BADGE_SIZE / 2))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(BADGE_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
                .clickable(onClick = onRemove),
        ) {
            Icon(
                painterResource(R.drawable.ic_minus),
                stringResource(R.string.remove_app, app.label),
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** A list of [apps] to pick one from. */
@Composable
private fun AddAppDialog(apps: List<InstalledApp>, onAdd: (InstalledApp) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_app)) },
        text = {
            LazyColumn {
                items(apps, key = { it.packageName }) { app ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAdd(app) }
                            .padding(vertical = 8.dp),
                    ) {
                        // The label next to it already names the app
                        Image(app.icon, contentDescription = null, modifier = Modifier.size(40.dp))
                        Text(app.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** The shortest and longest routine, matching the start screen's picker. */
private val ROUTINE_DURATION_RANGE_MINUTES = FOCUS_DURATIONS_MINUTES.first()..FOCUS_DURATIONS_MINUTES.last()

/** Adds [step] minutes (negative to subtract) to [minutes], staying within 5 min to 3 h. */
internal fun stepRoutineDuration(minutes: Int, step: Int): Int = (minutes + step).coerceIn(ROUTINE_DURATION_RANGE_MINUTES)

/** A duration picker in 5 and 30 minute steps: −30 −5 on the left, +5 +30 on the right. */
@Composable
private fun PreciseDurationPicker(
    durationMinutes: Int,
    onStepMinutes: (minutes: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
            .padding(4.dp),
    ) {
        StepButton(-30, durationMinutes, onStepMinutes)
        StepButton(-5, durationMinutes, onStepMinutes)
        Text(
            text = formatDuration(durationMinutes),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
            textAlign = TextAlign.Center,
            // Wide enough for the longest label, so the buttons don't move as it changes
            modifier = Modifier.widthIn(min = 120.dp),
        )
        StepButton(5, durationMinutes, onStepMinutes)
        StepButton(30, durationMinutes, onStepMinutes)
    }
}

@Composable
private fun StepButton(minutes: Int, durationMinutes: Int, onStepMinutes: (minutes: Int) -> Unit) {
    IconButton(
        onClick = { onStepMinutes(minutes) },
        // Disabled once it can't change anything, like the start screen's picker at its ends
        enabled = stepRoutineDuration(durationMinutes, minutes) != durationMinutes,
    ) {
        Text(
            stringResource(if (minutes < 0) R.string.minus_minutes else R.string.plus_minutes, abs(minutes)),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditRoutinePreview() {
    HocusPocusFocusTheme {
        val apps = previewApps()
        EditRoutineScreen(
            Routine(0, RoutineIcon.SUN, "Morning", 60, apps.take(2).map { it.packageName }),
            installedApps = apps,
            onChange = {},
            onDelete = {},
            onBack = {},
        )
    }
}
