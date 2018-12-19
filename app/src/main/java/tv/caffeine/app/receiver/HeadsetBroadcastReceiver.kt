package tv.caffeine.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.core.content.getSystemService

private const val UNPLUGGED_STATE = 0
private const val PLUGGED_STATE = 1

class HeadsetBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val audioManager = context?.getSystemService<AudioManager>()
        if (intent?.action == Intent.ACTION_HEADSET_PLUG) {
            val state = intent.getIntExtra("state", UNPLUGGED_STATE)
            when(state) {
                UNPLUGGED_STATE -> audioManager?.isSpeakerphoneOn = true
                PLUGGED_STATE -> audioManager?.isSpeakerphoneOn = false
            }
        }
        // Ignore bluetooth: BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED
    }
}