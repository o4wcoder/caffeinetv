package tv.caffeine.app.appinit

import android.app.Application
import timber.log.Timber
import javax.inject.Inject

class TimberInitializer @Inject constructor(
    private val timberTree: Timber.Tree
) : AppInitializer {
    override fun init(application: Application) {
        Timber.plant(timberTree)
    }
}
