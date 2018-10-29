package tv.caffeine.app.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

class CaffeineFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String?) {
        super.onNewToken(token)
        Timber.d("New FCM token generated $token")
    }

    override fun onMessageReceived(message: RemoteMessage?) {
        super.onMessageReceived(message)
        Timber.d("FCM message received: $message")
        message?.let { msg ->
            Timber.d("notification.body = ${msg.notification?.body}")
            Timber.d("data = ${msg.data}")
        }
    }
}
