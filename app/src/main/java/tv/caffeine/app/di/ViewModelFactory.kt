package tv.caffeine.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.PaymentsClientService
import tv.caffeine.app.api.SearchService
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.explore.ExploreViewModel
import tv.caffeine.app.lobby.LoadLobbyUseCase
import tv.caffeine.app.lobby.LobbyViewModel
import tv.caffeine.app.notifications.NotificationsViewModel
import tv.caffeine.app.profile.MyProfileViewModel
import tv.caffeine.app.profile.ProfileViewModel
import tv.caffeine.app.profile.WalletViewModel
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.SettingsViewModel
import tv.caffeine.app.stage.DICatalogViewModel
import tv.caffeine.app.users.FollowersViewModel
import tv.caffeine.app.users.FollowingViewModel
import tv.caffeine.app.users.IgnoredUsersViewModel
import javax.inject.Inject

class ViewModelFactory @Inject constructor(
        private val loadLobbyUseCase: LoadLobbyUseCase,
        private val searchService: SearchService,
        private val usersService: UsersService,
        private val paymentsClientService: PaymentsClientService,
        private val accountsService: AccountsService,
        private val tokenStore: TokenStore,
        private val followManager: FollowManager,
        private val gson: Gson
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(LobbyViewModel::class.java) -> LobbyViewModel(loadLobbyUseCase)
            modelClass.isAssignableFrom(ExploreViewModel::class.java) -> ExploreViewModel(searchService, usersService, gson)
            modelClass.isAssignableFrom(NotificationsViewModel::class.java) -> NotificationsViewModel(usersService, tokenStore)
            modelClass.isAssignableFrom(DICatalogViewModel::class.java) -> DICatalogViewModel(paymentsClientService)
            modelClass.isAssignableFrom(WalletViewModel::class.java) -> WalletViewModel(paymentsClientService)
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(followManager)
            modelClass.isAssignableFrom(IgnoredUsersViewModel::class.java) -> IgnoredUsersViewModel(tokenStore, usersService)
            modelClass.isAssignableFrom(FollowingViewModel::class.java) -> FollowingViewModel(usersService)
            modelClass.isAssignableFrom(FollowersViewModel::class.java) -> FollowersViewModel(usersService)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(tokenStore, followManager)
            modelClass.isAssignableFrom(MyProfileViewModel::class.java) -> MyProfileViewModel(accountsService, usersService, tokenStore, followManager)
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        } as T
    }
}
