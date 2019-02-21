package tv.caffeine.app.notifications

import android.app.NotificationManager
import android.graphics.Bitmap
import androidx.core.content.getSystemService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.squareup.picasso.Picasso
import dagger.android.AndroidInjection
import timber.log.Timber
import tv.caffeine.app.analytics.Analytics
import tv.caffeine.app.analytics.AnalyticsEvent
import tv.caffeine.app.analytics.NotificationEvent
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.util.*
import java.io.IOException
import javax.inject.Inject

private const val NOTIFICATION_IMAGE_WIDTH = 1280
private const val NOTIFICATION_IMAGE_HEIGHT = 720

class CaffeineFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var analytics: Analytics
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var picasso: Picasso

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
    }

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
        val notificationEvent = NotificationEvent(NotificationEvent.Type.Received, message.id, message.tag)
        analytics.trackEvent(AnalyticsEvent.Notification(tokenStore.caid, notificationEvent))
        when (val imageUrl = message.imageUrl) {
            null -> notify(message)
            else -> {
                try {
                    val bitmap = picasso.load(imageUrl)
                            .resize(NOTIFICATION_IMAGE_WIDTH, NOTIFICATION_IMAGE_HEIGHT)
                            .get()
                    notify(message, bitmap)
                } catch (e: IOException) {
                    Timber.e(e)
                    notify(message)
                }
            }
        }
    }

    private fun notify(message: RemoteMessage, bitmap: Bitmap? = null) {
        val notification = message.buildNotification(applicationContext, bitmap)
        val notificationManager = getSystemService<NotificationManager>() ?: return
        notificationManager.notify(message.tag, message.numericId, notification)
    }
}
