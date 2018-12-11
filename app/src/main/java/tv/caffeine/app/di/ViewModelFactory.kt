package tv.caffeine.app.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.PaymentsClientService
import tv.caffeine.app.api.Realtime
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.auth.*
import tv.caffeine.app.explore.ExploreViewModel
import tv.caffeine.app.explore.FindBroadcastersUseCase
import tv.caffeine.app.lobby.LoadLobbyUseCase
import tv.caffeine.app.lobby.LobbyViewModel
import tv.caffeine.app.notifications.NotificationsViewModel
import tv.caffeine.app.profile.*
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.*
import tv.caffeine.app.stage.ChatViewModel
import tv.caffeine.app.stage.DICatalogViewModel
import tv.caffeine.app.stage.SendDigitalItemViewModel
import tv.caffeine.app.users.FollowersViewModel
import tv.caffeine.app.users.FollowingViewModel
import tv.caffeine.app.users.IgnoredUsersViewModel
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.wallet.DigitalItemRepository
import javax.inject.Inject

class ViewModelFactory @Inject constructor(
        private val context: Context,
        private val dispatchConfig: DispatchConfig,
        private val signInUseCase: SignInUseCase,
        private val acceptLegalUseCase: AcceptLegalUseCase,
        private val loadLobbyUseCase: LoadLobbyUseCase,
        private val findBroadcastersUseCase: FindBroadcastersUseCase,
        private val usersService: UsersService,
        private val accountsService: AccountsService,
        private val paymentsClientService: PaymentsClientService,
        private val tokenStore: TokenStore,
        private val followManager: FollowManager,
        private val transactionHistoryUseCase: TransactionHistoryUseCase,
        private val loadGoldBundlesUseCase: LoadGoldBundlesUseCase,
        private val purchaseGoldBundleUseCase: PurchaseGoldBundleUseCase,
        private val updateEmailUseCase: UpdateEmailUseCase,
        private val updatePasswordUseCase: UpdatePasswordUseCase,
        private val realtime: Realtime,
        private val uploadAvatarUseCase: UploadAvatarUseCase,
        private val digitalItemRepository: DigitalItemRepository,
        private val gson: Gson
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(SignInViewModel::class.java) -> SignInViewModel(dispatchConfig, signInUseCase) as T
            modelClass.isAssignableFrom(LegalAgreementViewModel::class.java) -> LegalAgreementViewModel(dispatchConfig, acceptLegalUseCase)
            modelClass.isAssignableFrom(LobbyViewModel::class.java) -> LobbyViewModel(dispatchConfig, followManager, loadLobbyUseCase) as T
            modelClass.isAssignableFrom(ExploreViewModel::class.java) -> ExploreViewModel(dispatchConfig, findBroadcastersUseCase)
            modelClass.isAssignableFrom(NotificationsViewModel::class.java) -> NotificationsViewModel(dispatchConfig, gson, usersService, tokenStore)
            modelClass.isAssignableFrom(DICatalogViewModel::class.java) -> DICatalogViewModel(dispatchConfig, digitalItemRepository)
            modelClass.isAssignableFrom(WalletViewModel::class.java) -> WalletViewModel(dispatchConfig, gson, paymentsClientService)
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(dispatchConfig, followManager)
            modelClass.isAssignableFrom(IgnoredUsersViewModel::class.java) -> IgnoredUsersViewModel(dispatchConfig, gson, tokenStore, usersService)
            modelClass.isAssignableFrom(IgnoreUserViewModel::class.java) -> IgnoreUserViewModel(dispatchConfig, tokenStore, usersService, gson)
            modelClass.isAssignableFrom(ReportUserViewModel::class.java) -> ReportUserViewModel(dispatchConfig, usersService, gson)
            modelClass.isAssignableFrom(FollowingViewModel::class.java) -> FollowingViewModel(dispatchConfig, gson, usersService)
            modelClass.isAssignableFrom(FollowersViewModel::class.java) -> FollowersViewModel(dispatchConfig, gson, usersService)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(dispatchConfig, tokenStore, followManager)
            modelClass.isAssignableFrom(NotificationSettingsViewModel::class.java) -> NotificationSettingsViewModel(dispatchConfig, accountsService, gson)
            modelClass.isAssignableFrom(DeleteAccountViewModel::class.java) -> DeleteAccountViewModel(dispatchConfig, accountsService, tokenStore, gson)
            modelClass.isAssignableFrom(MyProfileViewModel::class.java) -> MyProfileViewModel(dispatchConfig, tokenStore, followManager, uploadAvatarUseCase)
            modelClass.isAssignableFrom(TransactionHistoryViewModel::class.java) -> TransactionHistoryViewModel(dispatchConfig, transactionHistoryUseCase)
            modelClass.isAssignableFrom(GoldBundlesViewModel::class.java) -> GoldBundlesViewModel(dispatchConfig, loadGoldBundlesUseCase, purchaseGoldBundleUseCase)
            modelClass.isAssignableFrom(UpdateProfileViewModel::class.java) -> UpdateProfileViewModel(dispatchConfig, updateEmailUseCase, updatePasswordUseCase)
            modelClass.isAssignableFrom(ChatViewModel::class.java) -> ChatViewModel(dispatchConfig, context, realtime, tokenStore, usersService, followManager, gson)
            modelClass.isAssignableFrom(SendDigitalItemViewModel::class.java) -> SendDigitalItemViewModel(dispatchConfig, gson, digitalItemRepository, paymentsClientService)
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        } as T
    }
}
