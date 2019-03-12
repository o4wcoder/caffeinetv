package tv.caffeine.app.di

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import androidx.recyclerview.widget.RecyclerView
import dagger.Module
import dagger.Provides
import tv.caffeine.app.R
import tv.caffeine.app.util.CropBorderedCircleTransformation
import tv.caffeine.app.util.UserTheme
import javax.inject.Singleton

@Module
class UIModule {
    @Provides
    fun providesResources(context: Context) = context.resources

    @Provides
    fun providesRecycledViewPool() = RecyclerView.RecycledViewPool()

    @Provides
    @Singleton
    @BlueCircleTransformation
    fun providesCircleWithBlueBorderTransformation(resources: Resources)
            = CropBorderedCircleTransformation(resources.getColor(R.color.caffeine_blue, null),
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics))

    @Provides
    @Singleton
    @WhiteCircleTransformation
    fun providesCircleWithWhiteBorderTransformation(resources: Resources)
            = CropBorderedCircleTransformation(resources.getColor(R.color.white, null),
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics))

    @Provides
    @Singleton
    @ThemeFollowedExplore
    fun providesFollowedUserThemeExplore(@BlueCircleTransformation transformation: CropBorderedCircleTransformation)
            = UserTheme(transformation, R.style.ExploreUsername_Following)

    @Provides
    @Singleton
    @ThemeNotFollowedExplore
    fun providesNotFollowedUserThemeExplore(@WhiteCircleTransformation transformation: CropBorderedCircleTransformation)
            = UserTheme(transformation, R.style.ExploreUsername_NotFollowing)

    @Provides
    @Singleton
    @ThemeNotFollowedExploreDark
    fun providesNotFollowedUserThemeExploreDark(@WhiteCircleTransformation transformation: CropBorderedCircleTransformation)
            = UserTheme(transformation, R.style.ExploreUsername_NotFollowingDark)

    @Provides
    @Singleton
    @ThemeFollowedLobby
    fun providesFollowedUserThemeLobby(@BlueCircleTransformation transformation: CropBorderedCircleTransformation)
            = UserTheme(transformation, R.style.BroadcastCardUsername_Following)

    @Provides
    @Singleton
    @ThemeNotFollowedLobby
    fun providesNotFollowedUserThemeLobby(@WhiteCircleTransformation transformation: CropBorderedCircleTransformation)
            = UserTheme(transformation, R.style.BroadcastCardUsername_NotFollowing)

    @Provides
    @Singleton
    @ThemeFollowedLobbyLight
    fun providesFollowedUserThemeLobbyLight(@BlueCircleTransformation transformation: CropBorderedCircleTransformation)
            = UserTheme(transformation, R.style.BroadcastCardUsername_Following_Previous)

    @Provides
    @Singleton
    @ThemeNotFollowedLobbyLight
    fun providesNotFollowedUserThemeLobbyLight(@WhiteCircleTransformation transformation: CropBorderedCircleTransformation)
            = UserTheme(transformation, R.style.BroadcastCardUsername_NotFollowing_Previous)

    @Provides
    @Singleton
    @ThemeFollowedChat
    fun providesFollowedUserThemeChat(@BlueCircleTransformation transformation: CropBorderedCircleTransformation)
            = UserTheme(transformation, R.style.ChatMessageUsername_Following)

    @Provides
    @Singleton
    @ThemeNotFollowedChat
    fun providesNotFollowedUserThemeChat(@WhiteCircleTransformation transformation: CropBorderedCircleTransformation)
            = UserTheme(transformation, R.style.ChatMessageUsername_NotFollowing)
}
