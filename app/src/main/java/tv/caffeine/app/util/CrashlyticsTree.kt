package tv.caffeine.app.util

import android.util.Log
import com.crashlytics.android.Crashlytics
import kotlinx.coroutines.CancellationException
import timber.log.Timber

private val ignoredPriorities = arrayOf(Log.VERBOSE, Log.DEBUG, Log.INFO)
private const val PRIORITY = "priority"
private const val TAG = "tag"
private const val MESSAGE = "message"

class CrashlyticsTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority in ignoredPriorities) return
        if (t is CancellationException) return

        Crashlytics.setInt(PRIORITY, priority)
        Crashlytics.setString(TAG, tag)
        Crashlytics.setString(MESSAGE, message)

        val exception = t ?: Exception(message)
        Crashlytics.logException(exception)
    }

}
