package tv.caffeine.app.appinit

import android.app.Application
import tv.caffeine.app.analytics.Analytics
import javax.inject.Inject

class AnalyticsInitializer @Inject constructor(
        private val analytics: Analytics
) : AppInitializer {
    override fun init(application: Application) {
        analytics.initialize()
    }
}
