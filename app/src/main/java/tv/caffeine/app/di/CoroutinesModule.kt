package tv.caffeine.app.di

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import tv.caffeine.app.util.DispatchConfig
import javax.inject.Singleton

@Module
class CoroutinesModule {
    @Provides
    @Singleton
    fun providesDispatchConfig() = DispatchConfig(main = Dispatchers.Main, io = Dispatchers.IO)

    @Provides
    fun providesCoroutineScope(): CoroutineScope {
        val job = Job()
        return CoroutineScope(Dispatchers.IO + job)
    }
}
