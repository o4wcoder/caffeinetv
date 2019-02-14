package tv.caffeine.app.util

import android.content.Intent

private const val NOTIFICATION_ID = "NOTIFICATION_ID"
private const val NOTIFICATION_TAG = "NOTIFICATION_TAG"

var Intent.notificationId: String?
    get() = getStringExtra(NOTIFICATION_ID)
    set(value) {
        putExtra(NOTIFICATION_ID, value)
    }

var Intent.notificationTag: String?
    get() = getStringExtra(NOTIFICATION_TAG)
    set(value) {
        putExtra(NOTIFICATION_TAG, value)
    }
