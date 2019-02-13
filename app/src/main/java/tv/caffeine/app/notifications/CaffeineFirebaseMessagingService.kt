package tv.caffeine.app.notifications

import android.app.NotificationManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import androidx.core.content.getSystemService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import timber.log.Timber
import tv.caffeine.app.util.buildNotification
import tv.caffeine.app.util.id
import tv.caffeine.app.util.imageUrl
import tv.caffeine.app.util.tag

class CaffeineFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String?) {
        super.onNewToken(token)
        Timber.d("New FCM token generated $token")
    }

    override fun onMessageReceived(message: RemoteMessage?) {
        super.onMessageReceived(message)
        Timber.d("FCM message received: $message")
        if (message == null) return
        Timber.d("notification.body = ${message.notification?.body}")
        Timber.d("data = ${message.data}")
        when (val imageUrl = message.imageUrl) {
            null -> notify(message)
            else -> {
                val loader = NotificationImageLoader { bitmap -> notify(message, bitmap) }
                val handler = Handler(Looper.getMainLooper())
                handler.post { Picasso.get().load(imageUrl).into(loader) }
            }
        }
    }

    private fun notify(message: RemoteMessage, bitmap: Bitmap? = null) {
        val notification = message.buildNotification(applicationContext, bitmap)
        val notificationManager = getSystemService<NotificationManager>() ?: return
        notificationManager.notify(message.tag, message.id, notification)
    }
}

class NotificationImageLoader(private val callback: (Bitmap?) -> Unit) : Target {
    override fun onPrepareLoad(placeHolderDrawable: Drawable?) = Unit
    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) = callback(null)
    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) = callback(bitmap)
}
