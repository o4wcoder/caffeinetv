package tv.caffeine.app.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import tv.caffeine.app.auth.*
import tv.caffeine.app.explore.ExploreFragment
import tv.caffeine.app.lobby.LobbyFragment
import tv.caffeine.app.notifications.NotificationsFragment
import tv.caffeine.app.profile.*
import tv.caffeine.app.settings.GoldAndCreditsFragment
import tv.caffeine.app.settings.GoldBundlesFragment
import tv.caffeine.app.settings.SettingsFragment
import tv.caffeine.app.settings.TransactionHistoryFragment
import tv.caffeine.app.stage.*
import tv.caffeine.app.users.FollowersFragment
import tv.caffeine.app.users.FollowingFragment
import tv.caffeine.app.users.IgnoredUsersFragment

@Module
abstract class InjectionModule {
    @ContributesAndroidInjector abstract fun signInFragment(): SignInFragment
    @ContributesAndroidInjector abstract fun signUpFragment(): SignUpFragment
    @ContributesAndroidInjector abstract fun landingFragment(): LandingFragment
    @ContributesAndroidInjector abstract fun forgotFragment(): ForgotFragment
    @ContributesAndroidInjector abstract fun lobbyFragment(): LobbyFragment
    @ContributesAndroidInjector abstract fun stageFragment(): StageFragment
    @ContributesAndroidInjector abstract fun mfaCodeFragment(): MfaCodeFragment
    @ContributesAndroidInjector abstract fun myProfileFragment(): MyProfileFragment
    @ContributesAndroidInjector abstract fun profileFragment(): ProfileFragment
    @ContributesAndroidInjector abstract fun exploreFragment(): ExploreFragment
    @ContributesAndroidInjector abstract fun notificationsFragment(): NotificationsFragment
    @ContributesAndroidInjector abstract fun friendsWatchingFragment(): FriendsWatchingFragment
    @ContributesAndroidInjector abstract fun digitalItemListDialogFragment(): DICatalogFragment
    @ContributesAndroidInjector abstract fun ignoredUsersFragment(): IgnoredUsersFragment
    @ContributesAndroidInjector abstract fun reportOrIgnoreDialogFragment(): ReportOrIgnoreDialogFragment
    @ContributesAndroidInjector abstract fun reportDialogFragment(): ReportDialogFragment
    @ContributesAndroidInjector abstract fun followersFragment(): FollowersFragment
    @ContributesAndroidInjector abstract fun followingFragment(): FollowingFragment
    @ContributesAndroidInjector abstract fun goldAndCreditsFragment(): GoldAndCreditsFragment
    @ContributesAndroidInjector abstract fun settingsFragment(): SettingsFragment
    @ContributesAndroidInjector abstract fun transactionHistoryFragment(): TransactionHistoryFragment
    @ContributesAndroidInjector abstract fun editBioFragment(): EditBioFragment
    @ContributesAndroidInjector abstract fun goldBundlesFragment(): GoldBundlesFragment
    @ContributesAndroidInjector abstract fun updateEmailFragment(): UpdateEmailFragment
    @ContributesAndroidInjector abstract fun updatePasswordFragment(): UpdatePasswordFragment
    @ContributesAndroidInjector abstract fun deleteAccountDialogFragment(): DeleteAccountDialogFragment
    @ContributesAndroidInjector abstract fun sendDigitalItemFragment(): SendDigitalItemFragment
    @ContributesAndroidInjector abstract fun sendMessageFragment(): SendMessageFragment
    @ContributesAndroidInjector abstract fun legalAgreementFragment(): LegalAgreementFragment
    @ContributesAndroidInjector abstract fun twitterAuthFragment(): TwitterAuthFragment
}
