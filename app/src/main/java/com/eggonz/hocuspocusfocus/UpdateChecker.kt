package com.eggonz.hocuspocusfocus

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val LATEST_RELEASE_API_URL = "https://api.github.com/repos/eggonz/hocus-pocus-focus/releases/latest"
private const val TIMEOUT_MILLIS = 10_000

/** Downloads the newest release's APK. */
const val LATEST_APK_URL = "https://github.com/eggonz/hocus-pocus-focus/releases/latest/download/hocus-pocus-focus.apk"

/** The latest release's version if newer than [currentVersion]; null if not, or if it can't be told (e.g. offline). */
suspend fun newerReleasedVersion(currentVersion: String): String? = withContext(Dispatchers.IO) {
    try {
        val connection = URL(LATEST_RELEASE_API_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MILLIS
        connection.readTimeout = TIMEOUT_MILLIS
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(response).getString("tag_name").removePrefix("v").takeIf { isNewerVersion(it, currentVersion) }
        } finally {
            connection.disconnect()
        }
    } catch (_: IOException) {
        null
    } catch (_: JSONException) {
        null
    }
}

/** Whether dotted version [candidate] is higher than [current]; false if either isn't made of numbers. */
fun isNewerVersion(candidate: String, current: String): Boolean {
    val candidateParts = candidate.split('.').map { it.toIntOrNull() ?: return false }
    val currentParts = current.split('.').map { it.toIntOrNull() ?: return false }
    for (index in 0 until maxOf(candidateParts.size, currentParts.size)) {
        val difference = candidateParts.getOrElse(index) { 0 } - currentParts.getOrElse(index) { 0 }
        if (difference != 0) return difference > 0
    }
    return false
}
