package tv.caffeine.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import tv.caffeine.app.api.PaymentsClientService
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.explore.ExploreViewModel
import tv.caffeine.app.explore.FindBroadcastersUseCase
import tv.caffeine.app.lobby.LoadLobbyUseCase
import tv.caffeine.app.lobby.LobbyViewModel
import tv.caffeine.app.notifications.NotificationsViewModel
import tv.caffeine.app.profile.*
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.*
import tv.caffeine.app.stage.DICatalogViewModel
import tv.caffeine.app.users.FollowersViewModel
import tv.caffeine.app.users.FollowingViewModel
import tv.caffeine.app.users.IgnoredUsersViewModel
import tv.caffeine.app.util.DispatchConfig
import javax.inject.Inject

class ViewModelFactory @Inject constructor(
        private val dispatchConfig: DispatchConfig,
        private val loadLobbyUseCase: LoadLobbyUseCase,
        private val findBroadcastersUseCase: FindBroadcastersUseCase,
        private val usersService: UsersService,
        private val paymentsClientService: PaymentsClientService,
        private val tokenStore: TokenStore,
        private val followManager: FollowManager,
        private val transactionHistoryUseCase: TransactionHistoryUseCase,
        private val loadGoldBundlesUseCase: LoadGoldBundlesUseCase,
        private val purchaseGoldBundleUseCase: PurchaseGoldBundleUseCase,
        private val updateEmailUseCase: UpdateEmailUseCase,
        private val updatePasswordUseCase: UpdatePasswordUseCase,
        private val uploadAvatarUseCase: UploadAvatarUseCase,
        private val gson: Gson
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(LobbyViewModel::class.java) -> LobbyViewModel(dispatchConfig, loadLobbyUseCase) as T
            modelClass.isAssignableFrom(ExploreViewModel::class.java) -> ExploreViewModel(dispatchConfig, findBroadcastersUseCase)
            modelClass.isAssignableFrom(NotificationsViewModel::class.java) -> NotificationsViewModel(dispatchConfig, usersService, tokenStore)
            modelClass.isAssignableFrom(DICatalogViewModel::class.java) -> DICatalogViewModel(dispatchConfig, paymentsClientService)
            modelClass.isAssignableFrom(WalletViewModel::class.java) -> WalletViewModel(dispatchConfig, paymentsClientService)
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(dispatchConfig, followManager)
            modelClass.isAssignableFrom(IgnoredUsersViewModel::class.java) -> IgnoredUsersViewModel(dispatchConfig, tokenStore, usersService)
            modelClass.isAssignableFrom(FollowingViewModel::class.java) -> FollowingViewModel(dispatchConfig, usersService)
            modelClass.isAssignableFrom(FollowersViewModel::class.java) -> FollowersViewModel(dispatchConfig, usersService)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(dispatchConfig, tokenStore, followManager)
            modelClass.isAssignableFrom(MyProfileViewModel::class.java) -> MyProfileViewModel(dispatchConfig, usersService, tokenStore, followManager, uploadAvatarUseCase, gson)
            modelClass.isAssignableFrom(TransactionHistoryViewModel::class.java) -> TransactionHistoryViewModel(dispatchConfig, transactionHistoryUseCase)
            modelClass.isAssignableFrom(GoldBundlesViewModel::class.java) -> GoldBundlesViewModel(dispatchConfig, loadGoldBundlesUseCase, purchaseGoldBundleUseCase)
            modelClass.isAssignableFrom(UpdateProfileViewModel::class.java) -> UpdateProfileViewModel(dispatchConfig, updateEmailUseCase, updatePasswordUseCase)
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        } as T
    }
}
