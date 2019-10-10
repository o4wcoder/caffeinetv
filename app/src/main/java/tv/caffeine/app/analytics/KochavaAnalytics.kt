package tv.caffeine.app.analytics

import androidx.annotation.VisibleForTesting
import androidx.navigation.NavDirections
import com.kochava.base.Tracker
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.api.model.IdentityProvider
import javax.inject.Inject

private fun IdentityProvider.toEventName() = when (this) {
    IdentityProvider.facebook -> "signin_fb_clicked"
    IdentityProvider.twitter -> "signin_twitter_clicked"
}

private const val NOTIFICATION_ID = "notification_id"
private const val NOTIFICATION_TAG = "notification_tag"
private const val NOTIFICATION_IS_DISPLAYED = "notification_is_displayed"
private const val ATTRIBUTION = "attribution"
private const val ATTRIBUTION_EMPTY = "false"
private const val USERNAME_KEY = "user"
private const val PAGE_KEY = "page"
private const val PAGE_STAGE = "stage"

class KochavaAnalytics @Inject constructor(
    private val configuration: Tracker.Configuration
) : Analytics {

    private var navDirections: NavDirections? = null

    override fun initialize() {
        Tracker.configure(configuration.setAttributionUpdateListener { handleAttribution(it) })
    }

    override fun trackEvent(event: AnalyticsEvent) {
        val trackerEvent = when (event) {
            is AnalyticsEvent.NewRegistration -> Tracker.Event(Tracker.EVENT_TYPE_REGISTRATION_COMPLETE).setUserId(event.userId)
            is AnalyticsEvent.SocialSignInClicked -> Tracker.Event(event.identityProvider.toEventName())
            is AnalyticsEvent.Notification -> Tracker.Event(
                    when (event.notification.type) {
                        NotificationEvent.Type.Received -> Tracker.EVENT_TYPE_PUSH_RECEIVED
                        NotificationEvent.Type.Opened -> Tracker.EVENT_TYPE_PUSH_OPENED
                    })
                    .addCustom(NOTIFICATION_ID, event.notification.id ?: "")
                    .addCustom(NOTIFICATION_TAG, event.notification.tag ?: "")
                    .addCustom(NOTIFICATION_IS_DISPLAYED, event.notification.isDisplayed)
                    .setUserId(event.userId ?: "")
            is AnalyticsEvent.NewAccountClicked -> Tracker.Event("new_account_clicked")
        }
        Tracker.sendEvent(trackerEvent)
    }

    @VisibleForTesting fun handleAttribution(attribution: String) {
        // TODO: remove the log and add tests once we confirm that the attribution works.
        Timber.e("Kochava attribution: $attribution")
        try {
            val attributionObject = JSONObject(attribution)
            if (attributionObject.optString(ATTRIBUTION, ATTRIBUTION_EMPTY) != ATTRIBUTION_EMPTY) {
                val username = attributionObject.optString(USERNAME_KEY, null)
                val page = attributionObject.optString(PAGE_KEY, null)
                if (username != null && page == PAGE_STAGE) {
                    navDirections = MainNavDirections.actionGlobalStagePagerFragment(username)
                }
            }
        } catch (exception: JSONException) {
            Timber.e(exception)
        }
    }

    override fun getDeferredDeeplinkNavDirections() = navDirections
}
