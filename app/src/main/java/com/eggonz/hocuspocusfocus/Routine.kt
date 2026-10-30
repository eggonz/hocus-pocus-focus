package com.eggonz.hocuspocusfocus

import com.eggonz.hocuspocusfocus.ui.IconPool
import org.json.JSONArray
import org.json.JSONObject

/** A step of a routine, shown as [icon] and [label]. */
data class RoutineTask(val icon: IconPool, val label: String)

/** A preset focus session the user can edit. [allowedApps] holds package names. */
data class Routine(
    val id: Int,
    val icon: IconPool,
    val title: String,
    val durationMinutes: Int,
    val allowedApps: List<String> = emptyList(),
    val tasks: List<RoutineTask> = emptyList(),
)

internal fun List<Routine>.toJson(): String = JSONArray(
    map {
        JSONObject()
            .put("id", it.id)
            .put("icon", it.icon.name)
            .put("title", it.title)
            .put("duration_minutes", it.durationMinutes)
            .put("allowed_apps", JSONArray(it.allowedApps))
            .put(
                "tasks",
                JSONArray(it.tasks.map { task -> JSONObject().put("icon", task.icon.name).put("label", task.label) }),
            )
    },
).toString()

internal fun routinesFromJson(json: String): List<Routine> {
    val array = JSONArray(json)
    return List(array.length()) { index ->
        val routine = array.getJSONObject(index)
        Routine(
            id = routine.getInt("id"),
            icon = iconNamed(routine.getString("icon"), fallback = IconPool.DEFAULT),
            title = routine.getString("title"),
            durationMinutes = routine.getInt("duration_minutes"),
            // Absent in routines saved before allowed apps existed
            allowedApps = routine.optJSONArray("allowed_apps")
                ?.let { apps -> List(apps.length()) { apps.getString(it) } }
                .orEmpty(),
            // Absent in routines saved before tasks existed
            tasks = routine.optJSONArray("tasks")
                ?.let { tasks -> List(tasks.length()) { taskFromJson(tasks.getJSONObject(it)) } }
                .orEmpty(),
        )
    }
}

private fun taskFromJson(task: JSONObject) = RoutineTask(
    icon = iconNamed(task.getString("icon"), fallback = IconPool.DEFAULT_TASK),
    label = task.getString("label"),
)

private fun iconNamed(name: String, fallback: IconPool): IconPool =
    IconPool.entries.find { it.name == name } ?: fallback
