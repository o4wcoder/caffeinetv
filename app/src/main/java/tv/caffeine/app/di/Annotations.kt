package tv.caffeine.app.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class BlueCircleTransformation

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class WhiteCircleTransformation

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class RoundedRectTransformation

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ThemeFollowedExplore

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ThemeNotFollowedExplore

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ThemeFollowedLobby

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ThemeNotFollowedLobby


