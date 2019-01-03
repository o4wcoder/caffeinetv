package tv.caffeine.app.receiver

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import androidx.core.content.getSystemService
import tv.caffeine.app.stage.NewReyesController

class AudioContentObserver(private val controller: NewReyesController, context: Context, handler: Handler) : ContentObserver(handler) {

    private val audioManager: AudioManager? = context.getSystemService()
    private val minVolume = audioManager?.getStreamMinVolume(AudioManager.STREAM_VOICE_CALL)

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