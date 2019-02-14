package tv.caffeine.app.util

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.RemoteMessage
import tv.caffeine.app.MainActivity
import tv.caffeine.app.R

private const val TITLE = "title"
private const val BODY = "body"
private const val IMAGE_URL = "image_url"
private const val DEEP_LINK = "link"
private const val TAG = "tag"
private const val ID = "id"

val RemoteMessage.imageUrl get() = data[IMAGE_URL]
val RemoteMessage.tag get() = data[TAG]
val RemoteMessage.id get() = data[ID]
val RemoteMessage.numericId get() = data[ID]?.toIntOrNull() ?: 0

fun RemoteMessage.buildNotification(context: Context, largeImage: Bitmap? = null): Notification? {
    val title = data[TITLE]
    val body = data[BODY]
    val deepLink = data[DEEP_LINK]
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink), context, MainActivity::class.java)
    intent.notificationId = id
    intent.notificationTag = tag
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    return NotificationCompat.Builder(context, "general")
            .setSmallIcon(R.drawable.caffeine_wireframe_logo)
            .setColor(ContextCompat.getColor(context, R.color.caffeine_blue))
            .setTicker(title)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .apply {
                if (largeImage != null) {
                    setLargeIcon(largeImage)
                    setStyle(NotificationCompat.BigPictureStyle().bigPicture(largeImage).bigLargeIcon(null))
                }
            }
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
}
