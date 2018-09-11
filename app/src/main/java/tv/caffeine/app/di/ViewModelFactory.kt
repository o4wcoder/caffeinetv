package tv.caffeine.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import tv.caffeine.app.api.LobbyService
import tv.caffeine.app.api.SearchService
import tv.caffeine.app.explore.ExploreViewModel
import tv.caffeine.app.lobby.LobbyViewModel
import javax.inject.Inject

class ViewModelFactory @Inject constructor(
        private val searchService: SearchService,
        private val lobbyService: LobbyService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(LobbyViewModel::class.java) -> LobbyViewModel(lobbyService) as T
            modelClass.isAssignableFrom(ExploreViewModel::class.java) -> ExploreViewModel(searchService) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
