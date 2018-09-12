package tv.caffeine.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import tv.caffeine.app.api.LobbyService
import tv.caffeine.app.api.SearchService
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.explore.ExploreViewModel
import tv.caffeine.app.lobby.LobbyViewModel
import tv.caffeine.app.notifications.NotificationsViewModel
import javax.inject.Inject

class ViewModelFactory @Inject constructor(
        private val lobbyService: LobbyService,
        private val searchService: SearchService,
        private val usersService: UsersService,
        private val tokenStore: TokenStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(LobbyViewModel::class.java) -> LobbyViewModel(lobbyService)
            modelClass.isAssignableFrom(ExploreViewModel::class.java) -> ExploreViewModel(searchService, usersService)
            modelClass.isAssignableFrom(NotificationsViewModel::class.java) -> NotificationsViewModel(usersService, tokenStore)
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        } as T
    }
}
