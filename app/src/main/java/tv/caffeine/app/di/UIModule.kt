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
            = CropBorderedCircleTransformation(resources.getColor(R.color.caffeineBlue, null),
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics))

    @Provides
    @Singleton
    @WhiteCircleTransformation
    fun providesCircleWithWhiteBorderTransformation(resources: Resources)
            = CropBorderedCircleTransformation(resources.getColor(R.color.white, null),
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics))

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
    @ThemeFollowedLobby
    fun providesFollowedUserThemeLobby(@BlueCircleTransformation transformation: CropBorderedCircleTransformation)
            = UserTheme(transformation, R.style.BroadcastCardUsername_Following)

    @Provides
    @Singleton
    @ThemeNotFollowedLobby
    fun providesNotFollowedUserThemeLobby(@WhiteCircleTransformation transformation: CropBorderedCircleTransformation)
            = UserTheme(transformation, R.style.BroadcastCardUsername_NotFollowing)
}
