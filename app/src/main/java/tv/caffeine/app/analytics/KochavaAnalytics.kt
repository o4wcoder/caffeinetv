package tv.caffeine.app.analytics

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.navigation.NavDirections
import com.google.gson.Gson
import com.kochava.base.Tracker
import org.json.JSONException
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
private const val PAGE_STAGE = "stage"

class KochavaAnalytics @Inject constructor(
    private val configuration: Tracker.Configuration,
    private val gson: Gson
) : Analytics {

    private var navDirections: NavDirections? = null

    override fun initialize() {
        // TODO: remove the log once we confirm that the attribution works.
        Log.d("Caffeine", "Kochava initialized")
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

    @VisibleForTesting fun handleAttribution(attributionString: String) {
        // TODO: remove the log once we confirm that the attribution works.
        Log.d("Caffeine", "Kochava initialized: $attributionString")
        try {
            val attribution = gson.fromJson(attributionString, DeeplinkAttribution::class.java)
            if (attribution.attribution == true) {
                if (attribution.user != null && attribution.page == PAGE_STAGE) {
                    navDirections = MainNavDirections.actionGlobalStagePagerFragment(attribution.user)
                }
            }
        } catch (exception: JSONException) {
            Timber.e(exception)
        }
    }

    override fun handleDeferredDeeplink(block: (directions: NavDirections?) -> Unit) {
        val navDirectionsToReturn = navDirections
        navDirections = null
        block(navDirectionsToReturn)
    }

    data class DeeplinkAttribution(
        val attribution: Boolean?,
        val page: String?,
        val user: String?
    )
}
