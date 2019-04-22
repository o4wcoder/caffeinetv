package tv.caffeine.app.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.facebook.login.LoginManager
import com.google.gson.Gson
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.BroadcastsService
import tv.caffeine.app.api.OAuthService
import tv.caffeine.app.api.PaymentsClientService
import tv.caffeine.app.api.Realtime
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.auth.AcceptLegalUseCase
import tv.caffeine.app.auth.LegalAgreementViewModel
import tv.caffeine.app.auth.SignInUseCase
import tv.caffeine.app.auth.SignInViewModel
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.broadcast.GuideViewModel
import tv.caffeine.app.broadcast.LiveHostableBroadcastersViewModel
import tv.caffeine.app.explore.ExploreViewModel
import tv.caffeine.app.explore.FindBroadcastersUseCase
import tv.caffeine.app.feature.LoadFeatureConfigUseCase
import tv.caffeine.app.lobby.FeaturedProgramGuideViewModel
import tv.caffeine.app.lobby.LoadFeaturedProgramGuideUseCase
import tv.caffeine.app.lobby.LoadLobbyUseCase
import tv.caffeine.app.lobby.LobbyViewModel
import tv.caffeine.app.notifications.NotificationsViewModel
import tv.caffeine.app.profile.DeleteAccountViewModel
import tv.caffeine.app.profile.IgnoreUserViewModel
import tv.caffeine.app.profile.MyProfileViewModel
import tv.caffeine.app.profile.ProfileViewModel
import tv.caffeine.app.profile.ReportUserViewModel
import tv.caffeine.app.profile.UpdateEmailUseCase
import tv.caffeine.app.profile.UpdatePasswordUseCase
import tv.caffeine.app.profile.UpdateProfileViewModel
import tv.caffeine.app.profile.UploadAvatarUseCase
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.session.SessionCheckViewModel
import tv.caffeine.app.settings.GoldBundlesViewModel
import tv.caffeine.app.settings.LoadGoldBundlesUseCase
import tv.caffeine.app.settings.NotificationSettingsViewModel
import tv.caffeine.app.settings.ProcessPlayStorePurchaseUseCase
import tv.caffeine.app.settings.PurchaseGoldBundleUseCase
import tv.caffeine.app.settings.SettingsViewModel
import tv.caffeine.app.settings.TransactionHistoryUseCase
import tv.caffeine.app.settings.TransactionHistoryViewModel
import tv.caffeine.app.stage.ChatViewModel
import tv.caffeine.app.stage.DICatalogViewModel
import tv.caffeine.app.stage.FriendsWatchingController
import tv.caffeine.app.stage.FriendsWatchingViewModel
import tv.caffeine.app.stage.MessageHandshake
import tv.caffeine.app.stage.SendDigitalItemViewModel
import tv.caffeine.app.update.IsVersionSupportedCheckUseCase
import tv.caffeine.app.users.FollowersViewModel
import tv.caffeine.app.users.FollowingViewModel
import tv.caffeine.app.users.IgnoredUsersViewModel
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.wallet.DigitalItemRepository
import tv.caffeine.app.wallet.WalletRepository
import tv.caffeine.app.wallet.WalletViewModel
import javax.inject.Inject

class ViewModelFactory @Inject constructor(
        private val context: Context,
        private val dispatchConfig: DispatchConfig,
        private val signInUseCase: SignInUseCase,
        private val acceptLegalUseCase: AcceptLegalUseCase,
        private val loadLobbyUseCase: LoadLobbyUseCase,
        private val loadFeaturedProgramGuideUseCase: LoadFeaturedProgramGuideUseCase,
        private val loadFeatureConfigUseCase: LoadFeatureConfigUseCase,
        private val findBroadcastersUseCase: FindBroadcastersUseCase,
        private val usersService: UsersService,
        private val accountsService: AccountsService,
        private val paymentsClientService: PaymentsClientService,
        private val tokenStore: TokenStore,
        private val followManager: FollowManager,
        private val transactionHistoryUseCase: TransactionHistoryUseCase,
        private val loadGoldBundlesUseCase: LoadGoldBundlesUseCase,
        private val purchaseGoldBundleUseCase: PurchaseGoldBundleUseCase,
        private val processPlayStorePurchaseUseCase: ProcessPlayStorePurchaseUseCase,
        private val updateEmailUseCase: UpdateEmailUseCase,
        private val updatePasswordUseCase: UpdatePasswordUseCase,
        private val realtime: Realtime,
        private val uploadAvatarUseCase: UploadAvatarUseCase,
        private val digitalItemRepository: DigitalItemRepository,
        private val walletRepository: WalletRepository,
        private val isVersionSupportedCheckUseCase: IsVersionSupportedCheckUseCase,
        private val oauthService: OAuthService,
        private val messageHandshakeFactory: MessageHandshake.Factory,
        private val friendsWatchingControllerFactory: FriendsWatchingController.Factory,
        private val broadcastsService: BroadcastsService,
        private val facebookLoginManager: LoginManager,
        private val gson: Gson
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(SignInViewModel::class.java) -> SignInViewModel(dispatchConfig, signInUseCase)
            modelClass.isAssignableFrom(LegalAgreementViewModel::class.java) -> LegalAgreementViewModel(dispatchConfig, acceptLegalUseCase)
            modelClass.isAssignableFrom(LobbyViewModel::class.java) -> LobbyViewModel(dispatchConfig, followManager, loadLobbyUseCase, loadFeatureConfigUseCase, isVersionSupportedCheckUseCase)
            modelClass.isAssignableFrom(SessionCheckViewModel::class.java) -> SessionCheckViewModel(dispatchConfig, tokenStore)
            modelClass.isAssignableFrom(ExploreViewModel::class.java) -> ExploreViewModel(dispatchConfig, findBroadcastersUseCase)
            modelClass.isAssignableFrom(NotificationsViewModel::class.java) -> NotificationsViewModel(dispatchConfig, gson, usersService, followManager, tokenStore)
            modelClass.isAssignableFrom(DICatalogViewModel::class.java) -> DICatalogViewModel(dispatchConfig, digitalItemRepository)
            modelClass.isAssignableFrom(WalletViewModel::class.java) -> WalletViewModel(dispatchConfig, walletRepository)
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(dispatchConfig, followManager)
            modelClass.isAssignableFrom(IgnoredUsersViewModel::class.java) -> IgnoredUsersViewModel(dispatchConfig, gson, tokenStore, usersService)
            modelClass.isAssignableFrom(IgnoreUserViewModel::class.java) -> IgnoreUserViewModel(dispatchConfig, tokenStore, usersService, gson)
            modelClass.isAssignableFrom(ReportUserViewModel::class.java) -> ReportUserViewModel(dispatchConfig, usersService, gson)
            modelClass.isAssignableFrom(FollowingViewModel::class.java) -> FollowingViewModel(dispatchConfig, gson, usersService)
            modelClass.isAssignableFrom(FollowersViewModel::class.java) -> FollowersViewModel(dispatchConfig, gson, usersService)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(dispatchConfig, tokenStore, followManager, usersService, oauthService, facebookLoginManager, gson)
            modelClass.isAssignableFrom(NotificationSettingsViewModel::class.java) -> NotificationSettingsViewModel(dispatchConfig, accountsService, gson)
            modelClass.isAssignableFrom(DeleteAccountViewModel::class.java) -> DeleteAccountViewModel(dispatchConfig, accountsService, tokenStore, gson)
            modelClass.isAssignableFrom(MyProfileViewModel::class.java) -> MyProfileViewModel(dispatchConfig, tokenStore, followManager, uploadAvatarUseCase)
            modelClass.isAssignableFrom(TransactionHistoryViewModel::class.java) -> TransactionHistoryViewModel(dispatchConfig, transactionHistoryUseCase)
            modelClass.isAssignableFrom(GoldBundlesViewModel::class.java) -> GoldBundlesViewModel(dispatchConfig, context, tokenStore, walletRepository, loadGoldBundlesUseCase, purchaseGoldBundleUseCase, processPlayStorePurchaseUseCase)
            modelClass.isAssignableFrom(UpdateProfileViewModel::class.java) -> UpdateProfileViewModel(dispatchConfig, updateEmailUseCase, updatePasswordUseCase)
            modelClass.isAssignableFrom(ChatViewModel::class.java) -> ChatViewModel(dispatchConfig, context, realtime, tokenStore, usersService, followManager, messageHandshakeFactory, gson)
            modelClass.isAssignableFrom(FriendsWatchingViewModel::class.java) -> FriendsWatchingViewModel(dispatchConfig, followManager, friendsWatchingControllerFactory)
            modelClass.isAssignableFrom(SendDigitalItemViewModel::class.java) -> SendDigitalItemViewModel(dispatchConfig, gson, digitalItemRepository, paymentsClientService)
            modelClass.isAssignableFrom(GuideViewModel::class.java) -> GuideViewModel(dispatchConfig, broadcastsService, gson)
            modelClass.isAssignableFrom(FeaturedProgramGuideViewModel::class.java) -> FeaturedProgramGuideViewModel(dispatchConfig, loadFeaturedProgramGuideUseCase)
            modelClass.isAssignableFrom(LiveHostableBroadcastersViewModel::class.java) -> LiveHostableBroadcastersViewModel(dispatchConfig, broadcastsService, gson)
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        } as T
    }
}
