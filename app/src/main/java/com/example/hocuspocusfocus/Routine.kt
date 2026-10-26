package com.example.hocuspocusfocus

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.json.JSONArray
import org.json.JSONObject

/** The icons a routine can have. Persisted by name, so renaming an entry resets routines that use it. */
enum class RoutineIcon(@param:DrawableRes val drawable: Int, @param:StringRes val label: Int) {
    SUN(R.drawable.ic_sun, R.string.icon_sun),
    MOON(R.drawable.ic_moon, R.string.icon_moon),
    BOOK(R.drawable.ic_book, R.string.icon_book),
}

/** A preset focus session the user can edit. [allowedApps] holds package names. */
data class Routine(
    val id: Int,
    val icon: RoutineIcon,
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
            icon = RoutineIcon.entries.find { it.name == routine.getString("icon") } ?: RoutineIcon.SUN,
            title = routine.getString("title"),
            durationMinutes = routine.getInt("duration_minutes"),
            // Absent in routines saved before allowed apps existed
            allowedApps = routine.optJSONArray("allowed_apps")
                ?.let { apps -> List(apps.length()) { apps.getString(it) } }
                .orEmpty(),
        )
    }
}
