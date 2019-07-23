package tv.caffeine.app.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import tv.caffeine.app.MainActivity
import tv.caffeine.app.broadcast.BroadcastFragment
import tv.caffeine.app.broadcast.LiveBroadcastPickerFragment
import tv.caffeine.app.broadcast.UpcomingBroadcastFragment
import tv.caffeine.app.explore.ExploreFragment
import tv.caffeine.app.lobby.FeaturedProgramGuideFragment
import tv.caffeine.app.lobby.LobbyFragment
import tv.caffeine.app.lobby.LobbySwipeFragment
import tv.caffeine.app.login.ArkoseFragment
import tv.caffeine.app.login.ConfirmEmailFragment
import tv.caffeine.app.login.ForgotFragment
import tv.caffeine.app.login.LandingFragment
import tv.caffeine.app.login.LegalAgreementFragment
import tv.caffeine.app.login.MfaCodeFragment
import tv.caffeine.app.login.SignInFragment
import tv.caffeine.app.login.SignUpFragment
import tv.caffeine.app.notifications.CaffeineFirebaseMessagingService
import tv.caffeine.app.notifications.NotificationsFragment
import tv.caffeine.app.profile.DeleteAccountDialogFragment
import tv.caffeine.app.profile.EditBioFragment
import tv.caffeine.app.profile.MyProfileFragment
import tv.caffeine.app.profile.ProfileFragment
import tv.caffeine.app.profile.ReportDialogFragment
import tv.caffeine.app.profile.ReportOrIgnoreDialogFragment
import tv.caffeine.app.settings.authentication.TwoStepAuthEmailFragment
import tv.caffeine.app.profile.UpdateEmailFragment
import tv.caffeine.app.profile.UpdatePasswordFragment
import tv.caffeine.app.settings.BuyGoldUsingCreditsDialogFragment
import tv.caffeine.app.settings.DisconnectIdentityDialogFragment
import tv.caffeine.app.settings.GoldAndCreditsFragment
import tv.caffeine.app.settings.GoldBundlesFragment
import tv.caffeine.app.settings.SettingsFragment
import tv.caffeine.app.settings.TransactionHistoryFragment
import tv.caffeine.app.settings.authentication.TwoStepAuthDisableDialogFragment
import tv.caffeine.app.settings.authentication.TwoStepAuthDoneFragment
import tv.caffeine.app.settings.authentication.TwoStepAuthEnableDialogFragment
import tv.caffeine.app.social.TwitterAuthForLogin
import tv.caffeine.app.social.TwitterAuthForSettings
import tv.caffeine.app.stage.ChatFragment
import tv.caffeine.app.stage.DICatalogFragment
import tv.caffeine.app.stage.FriendsWatchingFragment
import tv.caffeine.app.stage.SendDigitalItemFragment
import tv.caffeine.app.stage.SendMessageFragment
import tv.caffeine.app.stage.StageFragment
import tv.caffeine.app.stage.StagePagerFragment
import tv.caffeine.app.update.NeedsUpdateFragment
import tv.caffeine.app.users.FollowersFragment
import tv.caffeine.app.users.FollowingFragment
import tv.caffeine.app.users.IgnoredUsersFragment
import tv.caffeine.app.util.NoNetworkFragment

@Module
abstract class InjectionModule {
    @ContributesAndroidInjector(modules = [NavHostModule::class]) abstract fun mainActivity(): MainActivity
    @ContributesAndroidInjector abstract fun caffeineFirebaseMessagingService(): CaffeineFirebaseMessagingService
    @ContributesAndroidInjector abstract fun signInFragment(): SignInFragment
    @ContributesAndroidInjector abstract fun signUpFragment(): SignUpFragment
    @ContributesAndroidInjector abstract fun landingFragment(): LandingFragment
    @ContributesAndroidInjector abstract fun forgotFragment(): ForgotFragment
    @ContributesAndroidInjector abstract fun lobbySwipeFragment(): LobbySwipeFragment
    @ContributesAndroidInjector abstract fun lobbyFragment(): LobbyFragment
    @ContributesAndroidInjector abstract fun featuredProgramGuideFragment(): FeaturedProgramGuideFragment
    @ContributesAndroidInjector abstract fun stageFragment(): StageFragment
    @ContributesAndroidInjector abstract fun stagePagerFragment(): StagePagerFragment
    @ContributesAndroidInjector abstract fun broadcastFragment(): BroadcastFragment
    @ContributesAndroidInjector abstract fun liveBroadcastPickerFragment(): LiveBroadcastPickerFragment
    @ContributesAndroidInjector abstract fun upcomingBroadcastFragment(): UpcomingBroadcastFragment
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
    @ContributesAndroidInjector abstract fun buyGoldUsingCreditsDialogFragment(): BuyGoldUsingCreditsDialogFragment
    @ContributesAndroidInjector abstract fun settingsFragment(): SettingsFragment
    @ContributesAndroidInjector abstract fun disconnectIdentityDialogFragment(): DisconnectIdentityDialogFragment
    @ContributesAndroidInjector abstract fun transactionHistoryFragment(): TransactionHistoryFragment
    @ContributesAndroidInjector abstract fun editBioFragment(): EditBioFragment
    @ContributesAndroidInjector abstract fun goldBundlesFragment(): GoldBundlesFragment
    @ContributesAndroidInjector abstract fun updateEmailFragment(): UpdateEmailFragment
    @ContributesAndroidInjector abstract fun updatePasswordFragment(): UpdatePasswordFragment
    @ContributesAndroidInjector abstract fun deleteAccountDialogFragment(): DeleteAccountDialogFragment
    @ContributesAndroidInjector abstract fun sendDigitalItemFragment(): SendDigitalItemFragment
    @ContributesAndroidInjector abstract fun sendMessageFragment(): SendMessageFragment
    @ContributesAndroidInjector abstract fun legalAgreementFragment(): LegalAgreementFragment
    @ContributesAndroidInjector abstract fun twitterAuthForLogin(): TwitterAuthForLogin
    @ContributesAndroidInjector abstract fun twitterAuthForSettings(): TwitterAuthForSettings
    @ContributesAndroidInjector abstract fun needsUpdateFragment(): NeedsUpdateFragment
    @ContributesAndroidInjector abstract fun noNetworkFragment(): NoNetworkFragment
    @ContributesAndroidInjector abstract fun confirmEmailFragment(): ConfirmEmailFragment
    @ContributesAndroidInjector abstract fun updateAuthenticationFragment(): TwoStepAuthEmailFragment
    @ContributesAndroidInjector abstract fun arkoseFragment(): ArkoseFragment
    @ContributesAndroidInjector abstract fun twoStepAuthDisableDialogFragment(): TwoStepAuthDisableDialogFragment
    @ContributesAndroidInjector abstract fun twoStepAuthEnableDialogFragment(): TwoStepAuthEnableDialogFragment
    @ContributesAndroidInjector abstract fun twoStepAuthDoneFragment(): TwoStepAuthDoneFragment
    @ContributesAndroidInjector abstract fun classicChatFragment(): ChatFragment
}
