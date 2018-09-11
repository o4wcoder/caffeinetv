package tv.caffeine.app.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import tv.caffeine.app.ProfileFragment
import tv.caffeine.app.auth.*
import tv.caffeine.app.explore.ExploreFragment
import tv.caffeine.app.lobby.LobbyFragment
import tv.caffeine.app.stage.StageFragment

@Module
abstract class InjectionModule {
    @ContributesAndroidInjector abstract fun signInFragment(): SignInFragment
    @ContributesAndroidInjector abstract fun signUpFragment(): SignUpFragment
    @ContributesAndroidInjector abstract fun landingFragment(): LandingFragment
    @ContributesAndroidInjector abstract fun forgotFragment(): ForgotFragment
    @ContributesAndroidInjector abstract fun lobbyFragment(): LobbyFragment
    @ContributesAndroidInjector abstract fun stageFragment(): StageFragment
    @ContributesAndroidInjector abstract fun mfaCodeFragment(): MfaCodeFragment
    @ContributesAndroidInjector abstract fun profileFragment(): ProfileFragment
    @ContributesAndroidInjector abstract fun exploreFragment(): ExploreFragment
}
