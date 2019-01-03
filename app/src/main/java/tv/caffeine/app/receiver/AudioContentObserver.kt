package tv.caffeine.app.receiver

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import androidx.core.content.getSystemService
import tv.caffeine.app.stage.NewReyesController

class AudioContentObserver(private val controller: NewReyesController, context: Context, handler: Handler) : ContentObserver(handler) {

    private val audioManager: AudioManager? = context.getSystemService()
    private val minVolume = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        audioManager?.getStreamMinVolume(AudioManager.STREAM_VOICE_CALL)
    } else {
        1 // https://android.googlesource.com/platform/frameworks/base/+/master/services/core/java/com/android/server/audio/AudioService.java
    }

    override fun deliverSelfNotifications(): Boolean = false

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        audioManager?.let {
            if (it.getStreamVolume(AudioManager.STREAM_VOICE_CALL) == minVolume) {
                controller.mute()
            } else {
                controller.unmute()
            }
        }
    }
}