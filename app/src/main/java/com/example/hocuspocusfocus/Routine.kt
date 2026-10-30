package com.example.hocuspocusfocus

import com.example.hocuspocusfocus.ui.IconPool
import org.json.JSONArray
import org.json.JSONObject

/** A preset focus session the user can edit. [allowedApps] holds package names. */
data class Routine(
    val id: Int,
    val icon: IconPool,
    val title: String,
    val durationMinutes: Int,
    val allowedApps: List<String> = emptyList(),
)

internal fun List<Routine>.toJson(): String = JSONArray(
    map {
        JSONObject()
            .put("id", it.id)
            .put("icon", it.icon.name)
            .put("title", it.title)
            .put("duration_minutes", it.durationMinutes)
            .put("allowed_apps", JSONArray(it.allowedApps))
    },
).toString()

internal fun routinesFromJson(json: String): List<Routine> {
    val array = JSONArray(json)
    return List(array.length()) { index ->
        val routine = array.getJSONObject(index)
        Routine(
            id = routine.getInt("id"),
            icon = IconPool.entries.find { it.name == routine.getString("icon") } ?: IconPool.DEFAULT,
            title = routine.getString("title"),
            durationMinutes = routine.getInt("duration_minutes"),
            // Absent in routines saved before allowed apps existed
            allowedApps = routine.optJSONArray("allowed_apps")
                ?.let { apps -> List(apps.length()) { apps.getString(it) } }
                .orEmpty(),
        )
    }
}
