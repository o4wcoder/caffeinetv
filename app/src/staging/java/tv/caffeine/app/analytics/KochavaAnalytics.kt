package tv.caffeine.app.analytics

import com.kochava.base.Tracker
import javax.inject.Inject

class KochavaAnalytics @Inject constructor(
        private val configuration: Tracker.Configuration
) : Analytics {
    override fun initialize() {
        Tracker.configure(configuration)
    }

    override fun trackEvent(event: AnalyticsEvent) {
        when(event) {
            is AnalyticsEvent.NewRegistration -> Tracker.sendEvent(Tracker.Event(Tracker.EVENT_TYPE_REGISTRATION_COMPLETE).setUserId(event.userId))
        }
    }
}
