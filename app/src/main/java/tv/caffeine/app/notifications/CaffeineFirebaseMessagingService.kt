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
import tv.caffeine.app.util.buildNotification
import tv.caffeine.app.util.id
import tv.caffeine.app.util.imageUrl
import tv.caffeine.app.util.numericId
import tv.caffeine.app.util.tag
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
        val notificationManager = getSystemService<NotificationManager>()
        if (notificationManager == null) {
            val notificationDisplayedEvent = NotificationEvent(NotificationEvent.Type.Received, message.id, message.tag, false)
            analytics.trackEvent(AnalyticsEvent.Notification(tokenStore.caid, notificationDisplayedEvent))
            Timber.e("Notification Manager Service is not available")
            return
        }

        try {
            notificationManager.notify(message.tag, message.numericId, notification)
            val notificationDisplayedEvent = NotificationEvent(NotificationEvent.Type.Received, message.id, message.tag, true)
            analytics.trackEvent(AnalyticsEvent.Notification(tokenStore.caid, notificationDisplayedEvent))
        } catch (e: Exception) {
            val notificationDisplayedEvent = NotificationEvent(NotificationEvent.Type.Received, message.id, message.tag, false)
            analytics.trackEvent(AnalyticsEvent.Notification(tokenStore.caid, notificationDisplayedEvent))
            Timber.e(e)
        }
    }
}
