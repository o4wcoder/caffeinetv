package tv.caffeine.app.di

import androidx.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import tv.caffeine.app.broadcast.GuideViewModel
import tv.caffeine.app.broadcast.LiveHostableBroadcastersViewModel
import tv.caffeine.app.explore.ExploreViewModel
import tv.caffeine.app.lobby.FeaturedProgramGuideViewModel
import tv.caffeine.app.lobby.LobbyViewModel
import tv.caffeine.app.login.ArkoseViewModel
import tv.caffeine.app.login.LegalAgreementViewModel
import tv.caffeine.app.login.SignInViewModel
import tv.caffeine.app.login.SignUpViewModel
import tv.caffeine.app.notifications.NotificationCountViewModel
import tv.caffeine.app.notifications.NotificationsViewModel
import tv.caffeine.app.profile.DeleteAccountViewModel
import tv.caffeine.app.profile.IgnoreUserViewModel
import tv.caffeine.app.profile.MyProfileViewModel
import tv.caffeine.app.profile.ProfileViewModel
import tv.caffeine.app.profile.ReportUserViewModel
import tv.caffeine.app.profile.UpdateProfileViewModel
import tv.caffeine.app.session.SessionCheckViewModel
import tv.caffeine.app.settings.GoldBundlesViewModel
import tv.caffeine.app.settings.NotificationSettingsViewModel
import tv.caffeine.app.settings.SettingsViewModel
import tv.caffeine.app.settings.TransactionHistoryViewModel
import tv.caffeine.app.settings.authentication.TwoStepAuthViewModel
import tv.caffeine.app.social.TwitterAuthViewModel
import tv.caffeine.app.stage.ChatViewModel
import tv.caffeine.app.stage.DICatalogViewModel
import tv.caffeine.app.stage.FriendsWatchingViewModel
import tv.caffeine.app.stage.SendDigitalItemViewModel
import tv.caffeine.app.stage.StageBroadcastProfilePagerViewModel
import tv.caffeine.app.users.FollowersViewModel
import tv.caffeine.app.users.FollowingViewModel
import tv.caffeine.app.users.IgnoredUsersViewModel
import tv.caffeine.app.wallet.WalletViewModel

@Module
abstract class ViewModelBinds {
    @Binds
    @IntoMap
    @ViewModelKey(SignInViewModel::class)
    abstract fun bindSignInViewModel(viewModel: SignInViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SignUpViewModel::class)
    abstract fun bindSignUpViewModel(viewModel: SignUpViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LegalAgreementViewModel::class)
    abstract fun bindLegalAgreementViewModel(viewModel: LegalAgreementViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LobbyViewModel::class)
    abstract fun bindLobbyViewModel(viewModel: LobbyViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SessionCheckViewModel::class)
    abstract fun bindSessionCheckViewModel(viewModel: SessionCheckViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ExploreViewModel::class)
    abstract fun bindExploreViewModel(viewModel: ExploreViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NotificationsViewModel::class)
    abstract fun bindNotificationsViewModel(viewModel: NotificationsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DICatalogViewModel::class)
    abstract fun bindDICatalogViewModel(viewModel: DICatalogViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(WalletViewModel::class)
    abstract fun bindWalletViewModel(viewModel: WalletViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProfileViewModel::class)
    abstract fun bindProfileViewModel(viewModel: ProfileViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(IgnoredUsersViewModel::class)
    abstract fun bindIgnoredUsersViewModel(viewModel: IgnoredUsersViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(IgnoreUserViewModel::class)
    abstract fun bindIgnoreUserViewModel(viewModel: IgnoreUserViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ReportUserViewModel::class)
    abstract fun bindReportUserViewModel(viewModel: ReportUserViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FollowingViewModel::class)
    abstract fun bindFollowingViewModel(viewModel: FollowingViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FollowersViewModel::class)
    abstract fun bindFollowersViewModel(viewModel: FollowersViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    abstract fun bindSettingsViewModel(viewModel: SettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NotificationSettingsViewModel::class)
    abstract fun bindNotificationSettingsViewModel(viewModel: NotificationSettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DeleteAccountViewModel::class)
    abstract fun bindDeleteAccountViewModel(viewModel: DeleteAccountViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MyProfileViewModel::class)
    abstract fun bindMyProfileViewModel(viewModel: MyProfileViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TransactionHistoryViewModel::class)
    abstract fun bindTransactionHistoryViewModel(viewModel: TransactionHistoryViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(GoldBundlesViewModel::class)
    abstract fun bindGoldBundlesViewModel(viewModel: GoldBundlesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UpdateProfileViewModel::class)
    abstract fun bindUpdateProfileViewModel(viewModel: UpdateProfileViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ChatViewModel::class)
    abstract fun bindChatViewModel(viewModel: ChatViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FriendsWatchingViewModel::class)
    abstract fun bindFriendsWatchingViewModel(viewModel: FriendsWatchingViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SendDigitalItemViewModel::class)
    abstract fun bindSendDigitalItemViewModel(viewModel: SendDigitalItemViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(GuideViewModel::class)
    abstract fun bindGuideViewModel(viewModel: GuideViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FeaturedProgramGuideViewModel::class)
    abstract fun bindFeaturedProgramGuideViewModel(viewModel: FeaturedProgramGuideViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LiveHostableBroadcastersViewModel::class)
    abstract fun bindLiveHostableBroadcastersViewModel(viewModel: LiveHostableBroadcastersViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TwitterAuthViewModel::class)
    abstract fun bindTwitterAuthViewModel(viewModel: TwitterAuthViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ArkoseViewModel::class)
    abstract fun bindArkoseViewModel(viewModel: ArkoseViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TwoStepAuthViewModel::class)
    abstract fun bindTwoStepAuthDoneViewModel(viewModel: TwoStepAuthViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(StageBroadcastProfilePagerViewModel::class)
    abstract fun bindStageBroadcastDetailsPagerViewModel(viewModel: StageBroadcastProfilePagerViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NotificationCountViewModel::class)
    abstract fun bindNotificationCountViewModel(viewModel: NotificationCountViewModel): ViewModel
}
