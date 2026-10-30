package com.example.hocuspocusfocus.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hocuspocusfocus.FOCUS_DURATIONS_MINUTES
import com.example.hocuspocusfocus.R
import com.example.hocuspocusfocus.Routine
import com.example.hocuspocusfocus.RoutineTask
import com.example.hocuspocusfocus.ui.theme.DisplayFontFamily
import com.example.hocuspocusfocus.ui.theme.HocusPocusFocusTheme
import kotlin.math.abs

private val SCREEN_MARGIN = 24.dp
private val SECTION_SPACING = 32.dp
private val SECTION_HEADING_GAP = 20.dp
private val TASK_SPACING = 16.dp
private val ROW_ITEM_SPACING = 16.dp

// Fixed, so the duration picker's buttons don't move as its label changes: four 48dp step buttons, a label wide enough
// for the longest duration and the padding. Every section takes this width, to share both edges.
private val CONTENT_WIDTH = 320.dp

private val BADGE_SIZE = 24.dp
private val ICON_PICKER_SIZE = 56.dp

/** Centers something [APP_ICON_SIZE] wide on an icon picker with the same left edge. */
private val ICON_PICKER_INSET = (ICON_PICKER_SIZE - APP_ICON_SIZE) / 2

private const val APPS_PER_ROW = 4
private const val MAX_ALLOWED_APPS = 8
private const val MAX_TASKS = 20

// With APPS_PER_ROW icons and their badges' margin (60dp each), leaves the same 4dp at the grid's right as at its left;
// that spare room also stops pixel rounding from pushing the last icon onto the next row
private val APP_SPACING = 24.dp

/**
 * Edits a routine's icon (picked in a dialog), title, duration, allowed apps (picked from [installedApps], and counted
 * by [allowedAppCount], known before they have loaded) and tasks; every change is passed straight to [onChange]. The trash button at the top right calls [onDelete] once confirmed.
 */
@Composable
fun EditRoutineScreen(
    routine: Routine,
    allowedAppCount: Int,
    installedApps: List<InstalledApp>,
    onChange: (Routine) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmDeletion by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val measuredTaskCount = remember { mutableIntStateOf(routine.tasks.size) }

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
                verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
                modifier = Modifier
                    .fillMaxWidth()
                    .keepEndInPlaceOnAdd(scrollState, routine.tasks.size, measuredTaskCount)
                    .verticalScroll(scrollState)
                    .padding(horizontal = SCREEN_MARGIN, vertical = 16.dp)
                    // Centered, so the sections have the same margin on both sides
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .width(CONTENT_WIDTH),
            ) {
                IconAndTitle(routine, onChange)
                DurationSection(routine, onChange)
                AppsSection(routine, allowedAppCount, installedApps, onChange)
                TasksSection(routine.tasks, onChange = { onChange(routine.copy(tasks = it)) })
            }
        }
    }

    if (confirmDeletion) {
        AlertDialog(
            onDismissRequest = { confirmDeletion = false },
            title = { Text(stringResource(R.string.delete_routine)) },
            text = { Text(stringResource(R.string.delete_routine_message, routine.displayTitle())) },
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

}

/**
 * Scrolls by as much as adding an item grows the scroll range, so once the content overflows the screen, what's at its
 * end (the add button) stays where it is and the new item appears above it. Goes before [verticalScroll], so the range
 * is up to date and the scroll applies to the same frame, which never shows the button moved.
 */
private fun Modifier.keepEndInPlaceOnAdd(
    scrollState: ScrollState,
    itemCount: Int,
    measuredItemCount: MutableIntState,
) = layout { measurable, constraints ->
    val previousRange = Snapshot.withoutReadObservation { scrollState.maxValue }
    val placeable = measurable.measure(constraints)
    Snapshot.withoutReadObservation {
        if (itemCount > measuredItemCount.intValue) {
            scrollState.dispatchRawDelta((scrollState.maxValue - previousRange).toFloat())
        }
        measuredItemCount.intValue = itemCount
    }
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

/** The routine's icon, picked in a dialog, and its title. */
@Composable
private fun IconAndTitle(routine: Routine, onChange: (Routine) -> Unit) {
    var isChoosingIcon by rememberSaveable { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ROW_ITEM_SPACING),
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconPickerButton(
            routine.icon,
            onClick = { isChoosingIcon = true },
            // Lines up with the text field's outline, below its floating label
            modifier = Modifier.padding(top = 8.dp),
        )
        OutlinedTextField(
            value = routine.title,
            onValueChange = { onChange(routine.copy(title = it)) },
            label = { Text(stringResource(R.string.routine_title)) },
            placeholder = { Text(stringResource(R.string.new_routine)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }

    if (isChoosingIcon) {
        ChooseIconDialog(
            icon = routine.icon,
            quickIcons = quickRoutineIcons,
            onSave = { icon ->
                isChoosingIcon = false
                onChange(routine.copy(icon = icon))
            },
            onDismiss = { isChoosingIcon = false },
        )
    }
}

@Composable
private fun DurationSection(routine: Routine, onChange: (Routine) -> Unit) {
    EditSection(stringResource(R.string.routine_duration)) {
        PreciseDurationPicker(
            durationMinutes = routine.durationMinutes,
            onStepMinutes = { minutes ->
                onChange(routine.copy(durationMinutes = stepRoutineDuration(routine.durationMinutes, minutes)))
            },
        )
    }
}

/** A grid of the routine's allowed apps, with their count in the title since the add button goes away at the limit. */
@Composable
private fun AppsSection(
    routine: Routine,
    allowedAppCount: Int,
    installedApps: List<InstalledApp>,
    onChange: (Routine) -> Unit,
) {
    var isAddingApp by rememberSaveable { mutableStateOf(false) }
    val apps = appsIn(routine.allowedApps, installedApps)

    EditSection(
        stringResource(R.string.allowed_apps),
        detail = stringResource(R.string.section_count, allowedAppCount, MAX_ALLOWED_APPS),
    ) {
        FlowRow(
            maxItemsInEachRow = APPS_PER_ROW,
            horizontalArrangement = Arrangement.spacedBy(APP_SPACING),
            // As far apart as the icons are sideways, where their badges' margin adds to the spacing
            verticalArrangement = Arrangement.spacedBy(APP_SPACING + BADGE_SIZE / 2),
            modifier = Modifier
                .fillMaxWidth()
                // Centers the first column on the tasks' icon pickers and add button
                .padding(start = ICON_PICKER_INSET),
        ) {
            apps.forEach { app ->
                RemovableAppIcon(
                    app,
                    onRemove = { onChange(routine.copy(allowedApps = routine.allowedApps - app.packageName)) },
                )
            }
            // Counts installed apps only: an uninstalled one can't be removed, so it mustn't use up the limit
            if (allowedAppCount < MAX_ALLOWED_APPS) {
                AddButton(
                    stringResource(R.string.add_app),
                    onClick = { isAddingApp = true },
                    // Same slot as a removable icon, so it lines up with the icons
                    modifier = Modifier.padding(end = BADGE_SIZE / 2),
                )
            }
        }
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

/**
 * A [TaskRow] per task and an add button, with their count in the title since the add button goes away at the limit;
 * every change is passed straight to [onChange].
 */
@Composable
private fun TasksSection(tasks: List<RoutineTask>, onChange: (List<RoutineTask>) -> Unit) {
    var taskChoosingIcon by rememberSaveable { mutableStateOf<Int?>(null) }
    val focusManager = LocalFocusManager.current

    fun updateTask(index: Int, task: RoutineTask) = onChange(tasks.toMutableList().apply { set(index, task) })

    EditSection(
        stringResource(R.string.tasks),
        detail = stringResource(R.string.section_count, tasks.size, MAX_TASKS),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TASK_SPACING)) {
            tasks.forEachIndexed { index, task ->
                TaskRow(
                    task,
                    onChooseIcon = { taskChoosingIcon = index },
                    onLabelChange = { updateTask(index, task.copy(label = it)) },
                    onRemove = {
                        // Rows are positional, so focus would otherwise stay on the task that moves up
                        focusManager.clearFocus()
                        onChange(tasks.filterIndexed { other, _ -> other != index })
                    },
                )
            }
            if (tasks.size < MAX_TASKS) {
                AddButton(
                    stringResource(R.string.add_task),
                    onClick = { onChange(tasks + RoutineTask(IconPool.DEFAULT_TASK, "")) },
                    // Centered on the rows' icon pickers
                    modifier = Modifier.padding(start = ICON_PICKER_INSET),
                )
            }
        }
    }

    taskChoosingIcon?.let { index ->
        val task = tasks[index]
        ChooseIconDialog(
            icon = task.icon,
            quickIcons = quickTaskIcons,
            onSave = { icon ->
                taskChoosingIcon = null
                updateTask(index, task.copy(icon = icon))
            },
            onDismiss = { taskChoosingIcon = null },
        )
    }
}

/** A titled part of the screen above its [content], with an optional [detail] at the title's right. */
@Composable
private fun EditSection(title: String, detail: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SECTION_HEADING_GAP), modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            detail?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        content()
    }
}

/** The plus button that adds an item to a section, described by [label]. */
@Composable
private fun AddButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalIconButton(onClick = onClick, modifier = modifier.size(APP_ICON_SIZE)) {
        Icon(painterResource(R.drawable.ic_plus), label)
    }
}

/** A button showing [icon], which opens an icon dialog through [onClick]. */
@Composable
private fun IconPickerButton(icon: IconPool, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(ICON_PICKER_SIZE),
        shape = MaterialTheme.shapes.large,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Icon(painterResource(icon.drawable), stringResource(R.string.choose_icon))
    }
}

/** A task's icon picker, label field and a button that calls [onRemove]. */
@Composable
private fun TaskRow(
    task: RoutineTask,
    onChooseIcon: () -> Unit,
    onLabelChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconPickerButton(task.icon, onClick = onChooseIcon)
        OutlinedTextField(
            value = task.label,
            onValueChange = onLabelChange,
            placeholder = { Text(stringResource(R.string.task_label)) },
            singleLine = true,
            // Only on this side: the remove button's touch target already leaves a gap around its icon
            modifier = Modifier.padding(start = ROW_ITEM_SPACING).weight(1f),
        )
        IconButton(onClick = onRemove) {
            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.remove_task))
        }
    }
}

/** An app icon with a red minus badge at its top right that calls [onRemove]. */
@Composable
private fun RemovableAppIcon(app: InstalledApp, onRemove: () -> Unit) {
    Box {
        // Only sideways, so the badge overlaps the icon's top right corner
        AppIcon(app, modifier = Modifier.padding(end = BADGE_SIZE / 2))
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

private const val ICON_GRID_COLUMNS = 5
private val ICON_GRID_SPACING = 8.dp

/**
 * A grid of the [quickIcons] with [icon] selected, whose last cell opens [AllIconsDialog]; [onSave] gets the
 * one selected when Save is tapped. Tapping outside calls [onDismiss].
 */
@Composable
private fun ChooseIconDialog(
    icon: IconPool,
    quickIcons: List<IconPool>,
    onSave: (IconPool) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedIcon by rememberSaveable { mutableStateOf(icon) }
    var isShowingAllIcons by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ICON_GRID_SPACING)) {
                (quickIcons + null).chunked(ICON_GRID_COLUMNS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(ICON_GRID_SPACING)) {
                        row.forEach { icon ->
                            if (icon == null) {
                                // In the accent color so it doesn't pass for another icon; checked while the
                                // selected icon is only in the full pool, to show where it is
                                IconChoiceButton(
                                    isSelected = selectedIcon !in quickIcons,
                                    onSelect = { isShowingAllIcons = true },
                                    contentColor = MaterialTheme.colorScheme.primary,
                                ) {
                                    Icon(painterResource(R.drawable.ic_more), stringResource(R.string.all_icons))
                                }
                            } else {
                                IconChoice(icon, selectedIcon) { selectedIcon = it }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selectedIcon) }) { Text(stringResource(R.string.save)) }
        },
    )
    if (isShowingAllIcons) {
        AllIconsDialog(
            selectedIcon = selectedIcon,
            onSelect = { selectedIcon = it },
            onSave = { onSave(selectedIcon) },
            onDismiss = { isShowingAllIcons = false },
        )
    }
}

/**
 * Every [IconPool] entry under its category's title, in declaration order, with [selectedIcon] selected.
 * Tapping outside calls [onDismiss], going back to the quick grid.
 */
@Composable
private fun AllIconsDialog(
    selectedIcon: IconPool,
    onSelect: (IconPool) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val categories = remember { IconPool.entries.groupBy { it.category } }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(ICON_GRID_COLUMNS),
                horizontalArrangement = Arrangement.spacedBy(ICON_GRID_SPACING),
                verticalArrangement = Arrangement.spacedBy(ICON_GRID_SPACING),
                modifier = Modifier
                    // As wide as the quick grid, whose icon buttons take the minimum touch target size
                    .width(48.dp * ICON_GRID_COLUMNS + ICON_GRID_SPACING * (ICON_GRID_COLUMNS - 1))
                    .height(480.dp),
            ) {
                categories.forEach { (category, icons) ->
                    item(key = category, span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            categoryTitle(category),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(icons, key = { it.name }) { icon -> IconChoice(icon, selectedIcon, onSelect) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text(stringResource(R.string.save)) }
        },
    )
}

/** "art_and_crafts" as "Art and crafts". */
internal fun categoryTitle(category: String): String = category.replace('_', ' ').replaceFirstChar { it.uppercase() }

/** A button showing [icon], selected when it is [selectedIcon]. */
@Composable
private fun IconChoice(icon: IconPool, selectedIcon: IconPool, onSelect: (IconPool) -> Unit) {
    IconChoiceButton(isSelected = icon == selectedIcon, onSelect = { onSelect(icon) }) {
        Icon(painterResource(icon.drawable), icon.label?.let { stringResource(it) })
    }
}

/**
 * A cell of the icon grids, drawn in [contentColor]; only the selected one gets a background, with the icon cut out of
 * it for contrast.
 */
@Composable
private fun IconChoiceButton(
    isSelected: Boolean,
    onSelect: () -> Unit,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    content: @Composable () -> Unit,
) {
    FilledIconToggleButton(
        checked = isSelected,
        onCheckedChange = { onSelect() },
        colors = IconButtonDefaults.filledIconToggleButtonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            checkedContentColor = MaterialTheme.colorScheme.background,
        ),
        content = content,
    )
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
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .padding(4.dp),
    ) {
        StepButton(-30, durationMinutes, onStepMinutes)
        StepButton(-5, durationMinutes, onStepMinutes)
        Text(
            text = formatDuration(durationMinutes),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
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
private fun ChooseIconDialogPreview() {
    HocusPocusFocusTheme {
        ChooseIconDialog(IconPool.SUNRISE, quickRoutineIcons, onSave = {}, onDismiss = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun AllIconsDialogPreview() {
    HocusPocusFocusTheme {
        AllIconsDialog(IconPool.SUNRISE, onSelect = {}, onSave = {}, onDismiss = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun EditRoutinePreview() {
    HocusPocusFocusTheme {
        val apps = previewApps()
        EditRoutineScreen(
            Routine(
                0,
                IconPool.SUN,
                "Morning",
                60,
                apps.take(2).map { it.packageName },
                listOf(RoutineTask(IconPool.PILLS, "Vitamins"), RoutineTask(IconPool.DEFAULT_TASK, "")),
            ),
            allowedAppCount = 2,
            installedApps = apps,
            onChange = {},
            onDelete = {},
            onBack = {},
        )
    }
}
