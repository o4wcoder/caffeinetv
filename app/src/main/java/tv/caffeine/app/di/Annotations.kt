package tv.caffeine.app.di

import androidx.lifecycle.ViewModel
import dagger.MapKey
import javax.inject.Qualifier
import kotlin.reflect.KClass


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

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@MapKey
annotation class ViewModelKey(val value: KClass<out ViewModel>)
