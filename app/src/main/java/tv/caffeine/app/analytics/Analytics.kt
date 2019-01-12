package tv.caffeine.app.analytics

import timber.log.Timber
import tv.caffeine.app.api.model.IdentityProvider

interface Analytics {
    fun initialize()
    fun trackEvent(event: AnalyticsEvent)
}

class LogAnalytics : Analytics {
    override fun initialize() {
        Timber.d("LogAnalytics initialization")
    }

    override fun trackEvent(event: AnalyticsEvent) {
        when(event) {
            is AnalyticsEvent.NewRegistration -> Timber.d("New Registration, user ID = ${event.userId}")
        }
    }
}

sealed class AnalyticsEvent {
    class NewRegistration(val userId: String) : AnalyticsEvent()
    class SocialSignInClicked(val identityProvider: IdentityProvider) : AnalyticsEvent()
    object NewAccountClicked : AnalyticsEvent()
}
