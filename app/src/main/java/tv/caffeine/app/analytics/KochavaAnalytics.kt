package tv.caffeine.app.analytics

import com.kochava.base.Tracker
import tv.caffeine.app.api.model.IdentityProvider
import javax.inject.Inject

private fun IdentityProvider.toEventName() = when(this) {
    IdentityProvider.facebook -> "signin_fb_clicked"
    IdentityProvider.twitter -> "signin_twitter_clicked"
}

class KochavaAnalytics @Inject constructor(
        private val configuration: Tracker.Configuration
) : Analytics {
    override fun initialize() {
        Tracker.configure(configuration)
    }

    override fun trackEvent(event: AnalyticsEvent) {
        val trackerEvent = when(event) {
            is AnalyticsEvent.NewRegistration -> Tracker.Event(Tracker.EVENT_TYPE_REGISTRATION_COMPLETE).setUserId(event.userId)
            is AnalyticsEvent.SocialSignInClicked -> Tracker.Event(event.identityProvider.toEventName())
            AnalyticsEvent.NewAccountClicked -> Tracker.Event("new_account_clicked")
        }
        Tracker.sendEvent(trackerEvent)
    }
}
