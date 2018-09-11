package tv.caffeine.app.di

import dagger.Module
import dagger.Provides
import tv.caffeine.app.explore.ExploreAdapter
import tv.caffeine.app.session.FollowManager

@Module
class ViewModelModule {
    @Provides
    fun providesExploreAdapter(followManager: FollowManager): ExploreAdapter = ExploreAdapter(followManager)
}
