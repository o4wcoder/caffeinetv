package tv.caffeine.app.di

import android.content.Context
import android.content.res.Resources
import android.media.AudioManager
import androidx.recyclerview.widget.RecyclerView
import dagger.Module
import dagger.Provides

@Module(includes = [
    ViewModelBinds::class
])
class UIModule {
    @Provides
    fun providesResources(context: Context): Resources = context.resources

    @Provides
    fun providesRecycledViewPool() = RecyclerView.RecycledViewPool()

    @Provides
    fun providesAudioManager(context: Context): AudioManager = context.getSystemService(AudioManager::class.java)
}
