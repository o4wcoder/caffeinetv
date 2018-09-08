package tv.caffeine.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import tv.caffeine.app.api.LobbyService
import tv.caffeine.app.lobby.LobbyViewModel
import javax.inject.Inject

class LobbyViewModelFactory @Inject constructor(
        private val lobbyService: LobbyService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LobbyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LobbyViewModel(lobbyService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
