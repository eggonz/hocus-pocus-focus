package com.example.hocuspocusfocus

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri

/** Plays the end-of-session sound once, on the alarm stream so it's heard even with media muted. */
class AlarmPlayer(private val context: Context) {

    // Kept so the ringtone isn't garbage collected, and its player released, while it plays
    private var ringtone: Ringtone? = null

    fun play(sound: Uri) {
        ringtone?.stop()
        ringtone = RingtoneManager.getRingtone(context, sound)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            isLooping = false
            play()
        }
    }
}
