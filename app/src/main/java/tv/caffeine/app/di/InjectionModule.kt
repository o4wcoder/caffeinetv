package tv.caffeine.app.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import tv.caffeine.app.auth.ForgotFragment
import tv.caffeine.app.auth.LandingFragment
import tv.caffeine.app.auth.SignInFragment
import tv.caffeine.app.lobby.LobbyFragment

@Module
abstract class InjectionModule {
    @ContributesAndroidInjector abstract fun contributesSignInFragmentInjector(): SignInFragment
    @ContributesAndroidInjector abstract fun contributesLandingFragmentInjector(): LandingFragment
    @ContributesAndroidInjector abstract fun contributesForgotFragmentInjector(): ForgotFragment
    @ContributesAndroidInjector abstract fun contributesLobbyFragmentInjector(): LobbyFragment
}
