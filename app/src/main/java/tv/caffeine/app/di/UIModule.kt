package tv.caffeine.app.di

import android.content.Context
import android.content.res.Resources
import androidx.recyclerview.widget.RecyclerView
import dagger.Module
import dagger.Provides
import tv.caffeine.app.R
import tv.caffeine.app.util.UserTheme
import javax.inject.Singleton

@Module(includes = [
    UserThemeModule::class,
    ViewModelBinds::class
])
class UIModule {
    @Provides
    fun providesResources(context: Context): Resources = context.resources

    @Provides
    fun providesRecycledViewPool() = RecyclerView.RecycledViewPool()
}

@Module
class UserThemeModule {
    @Provides
    @Singleton
    @ThemeFollowedExplore
    fun providesFollowedUserThemeExplore() = UserTheme(R.style.ExploreUsername_Following)

    @Provides
    @Singleton
    @ThemeNotFollowedExplore
    fun providesNotFollowedUserThemeExplore() = UserTheme(R.style.ExploreUsername_NotFollowing)

    @Provides
    @Singleton
    @ThemeNotFollowedExploreDark
    fun providesNotFollowedUserThemeExploreDark() = UserTheme(R.style.ExploreUsername_NotFollowingDark)

    @Provides
    @Singleton
    @ThemeFollowedLobby
    fun providesFollowedUserThemeLobby() = UserTheme(R.style.BroadcastCardUsername_Following)

    @Provides
    @Singleton
    @ThemeNotFollowedLobby
    fun providesNotFollowedUserThemeLobby() = UserTheme(R.style.BroadcastCardUsername_NotFollowing)

    @Provides
    @Singleton
    @ThemeFollowedLobbyLight
    fun providesFollowedUserThemeLobbyLight() = UserTheme(R.style.BroadcastCardUsername_Following_Previous)

    @Provides
    @Singleton
    @ThemeNotFollowedLobbyLight
    fun providesNotFollowedUserThemeLobbyLight() = UserTheme(R.style.BroadcastCardUsername_NotFollowing_Previous)

    @Provides
    @Singleton
    @ThemeFollowedChat
    fun providesFollowedUserThemeChat() = UserTheme(R.style.ChatMessageUsername_Following)

    @Provides
    @Singleton
    @ThemeNotFollowedChat
    fun providesNotFollowedUserThemeChat() = UserTheme(R.style.ChatMessageUsername_NotFollowing)
}
