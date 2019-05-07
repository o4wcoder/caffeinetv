package tv.caffeine.app.di

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import tv.caffeine.app.auth.ConfirmEmailFragment
import tv.caffeine.app.auth.ForgotFragment
import tv.caffeine.app.auth.LandingFragment
import tv.caffeine.app.auth.LegalAgreementFragment
import tv.caffeine.app.auth.MfaCodeFragment
import tv.caffeine.app.auth.SignInFragment
import tv.caffeine.app.auth.SignUpFragment
import tv.caffeine.app.auth.TwitterAuthFragment
import tv.caffeine.app.broadcast.BroadcastFragment
import tv.caffeine.app.broadcast.LiveBroadcastPickerFragment
import tv.caffeine.app.broadcast.UpcomingBroadcastFragment
import tv.caffeine.app.explore.ExploreFragment
import tv.caffeine.app.lobby.FeaturedProgramGuideFragment
import tv.caffeine.app.lobby.LobbyFragment
import tv.caffeine.app.lobby.LobbySwipeFragment
import tv.caffeine.app.notifications.NotificationsFragment
import tv.caffeine.app.profile.DeleteAccountDialogFragment
import tv.caffeine.app.profile.EditBioFragment
import tv.caffeine.app.profile.MyProfileFragment
import tv.caffeine.app.profile.ProfileFragment
import tv.caffeine.app.profile.ReportDialogFragment
import tv.caffeine.app.profile.ReportOrIgnoreDialogFragment
import tv.caffeine.app.profile.UpdateEmailFragment
import tv.caffeine.app.profile.UpdatePasswordFragment
import tv.caffeine.app.settings.BuyGoldUsingCreditsDialogFragment
import tv.caffeine.app.settings.DisconnectIdentityDialogFragment
import tv.caffeine.app.settings.GoldAndCreditsFragment
import tv.caffeine.app.settings.GoldBundlesFragment
import tv.caffeine.app.settings.SettingsFragment
import tv.caffeine.app.settings.TransactionHistoryFragment
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
abstract class FragmentBindingModule {

    @Binds
    abstract fun bindFragmentFactory(factory: InjectingFragmentFactory): FragmentFactory

    @Binds
    @IntoMap
    @FragmentKey(SignInFragment::class)
    abstract fun bindSignInFragment(fragment: SignInFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(SignUpFragment::class)
    abstract fun bindSignUpFragment(fragment: SignUpFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(LandingFragment::class)
    abstract fun bindLandingFragment(fragment: LandingFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(ForgotFragment::class)
    abstract fun bindForgotFragment(fragment: ForgotFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(LobbySwipeFragment::class)
    abstract fun bindLobbySwipeFragment(fragment: LobbySwipeFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(LobbyFragment::class)
    abstract fun bindLobbyFragment(fragment: LobbyFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(FeaturedProgramGuideFragment::class)
    abstract fun bindfeaturedProgramGuideFragment(fragment: FeaturedProgramGuideFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(StageFragment::class)
    abstract fun bindStageFragment(fragment: StageFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(StagePagerFragment::class)
    abstract fun bindStagePagerFragment(fragment: StagePagerFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(BroadcastFragment::class)
    abstract fun bindBroadcastFragment(fragment: BroadcastFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(LiveBroadcastPickerFragment::class)
    abstract fun bindLiveBroadcastPickerFragment(fragment: LiveBroadcastPickerFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(UpcomingBroadcastFragment::class)
    abstract fun bindUpcomingBroadcastFragment(fragment: UpcomingBroadcastFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(MfaCodeFragment::class)
    abstract fun bindMfaCodeFragment(fragment: MfaCodeFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(MyProfileFragment::class)
    abstract fun bindMyProfileFragment(fragment: MyProfileFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(ExploreFragment::class)
    abstract fun bindExploreFragment(fragment: ExploreFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(NotificationsFragment::class)
    abstract fun bindNotificationsFragment(fragment: NotificationsFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(FriendsWatchingFragment::class)
    abstract fun bindFriendsWatchingFragment(fragment: FriendsWatchingFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(DICatalogFragment::class)
    abstract fun bindDICatalogFragment(fragment: DICatalogFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(IgnoredUsersFragment::class)
    abstract fun bindIgnoredUsersFragment(fragment: IgnoredUsersFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(FollowersFragment::class)
    abstract fun bindFollowersFragment(fragment: FollowersFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(FollowingFragment::class)
    abstract fun bindFollowingFragment(fragment: FollowingFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(GoldAndCreditsFragment::class)
    abstract fun bindGoldAndCreditsFragment(fragment: GoldAndCreditsFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(SettingsFragment::class)
    abstract fun bindSettingsFragment(fragment: SettingsFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(TransactionHistoryFragment::class)
    abstract fun bindTransactionHistoryFragment(fragment: TransactionHistoryFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(GoldBundlesFragment::class)
    abstract fun bindGoldBundlesFragment(fragment: GoldBundlesFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(SendDigitalItemFragment::class)
    abstract fun bindSendDigitalItemFragment(fragment: SendDigitalItemFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(TwitterAuthFragment::class)
    abstract fun bindTwitterAuthFragment(fragment: TwitterAuthFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(ConfirmEmailFragment::class)
    abstract fun bindConfirmEmailFragment(fragment: ConfirmEmailFragment): Fragment
}

