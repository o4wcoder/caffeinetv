package tv.caffeine.app.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import tv.caffeine.app.ProfileFragment
import tv.caffeine.app.auth.*
import tv.caffeine.app.lobby.LobbyFragment
import tv.caffeine.app.stage.StageFragment

@Module
abstract class InjectionModule {
    @ContributesAndroidInjector abstract fun contributesSignInFragmentInjector(): SignInFragment
    @ContributesAndroidInjector abstract fun contributesSignUpFragmentInjector(): SignUpFragment
    @ContributesAndroidInjector abstract fun contributesLandingFragmentInjector(): LandingFragment
    @ContributesAndroidInjector abstract fun contributesForgotFragmentInjector(): ForgotFragment
    @ContributesAndroidInjector abstract fun contributesLobbyFragmentInjector(): LobbyFragment
    @ContributesAndroidInjector abstract fun contributesStageFragmentInjector(): StageFragment
    @ContributesAndroidInjector abstract fun contributesMfaCodeFragmentInjector(): MfaCodeFragment
    @ContributesAndroidInjector abstract fun contributesProfileFragmentInjector(): ProfileFragment
}
