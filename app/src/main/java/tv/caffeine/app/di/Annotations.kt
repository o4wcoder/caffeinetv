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
annotation class ThemeNotFollowedExploreDark

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ThemeFollowedLobby

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ThemeNotFollowedLobby

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ThemeFollowedLobbyLight

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ThemeNotFollowedLobbyLight

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ThemeFollowedChat

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ThemeNotFollowedChat

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class CaffeineApi(val api: Service)

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ClientType(val authorizationType: AuthorizationType)
