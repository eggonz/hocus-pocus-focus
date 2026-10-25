package com.example.hocuspocusfocus

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper

/**
 * Plays the end-of-session sound once, as a short notice: it's cut off after [MAX_PLAY_MILLIS]
 * in case a long sound was picked. It plays on the alarm stream so it's heard even with media muted.
 */
class AlarmPlayer(private val context: Context) {

    // Kept so the ringtone isn't garbage collected, and its player released, while it plays
    private var ringtone: Ringtone? = null
    private val handler = Handler(Looper.getMainLooper())
    private val stop = Runnable { ringtone?.stop() }

    fun play(sound: Uri) {
        handler.removeCallbacks(stop)
        ringtone?.stop()
        ringtone = RingtoneManager.getRingtone(context, sound)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            isLooping = false
            play()
        }
        handler.postDelayed(stop, MAX_PLAY_MILLIS)
    }

    private companion object {
        const val MAX_PLAY_MILLIS = 4_000L
    }
}
