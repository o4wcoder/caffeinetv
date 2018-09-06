package tv.caffeine.app.di

import dagger.Module
import dagger.Provides
import tv.caffeine.app.api.LobbyService

@Module
class ViewModelModule {
    @Provides
    fun providesLobbyViewModelFactory(lobbyService: LobbyService): LobbyViewModelFactory
            = LobbyViewModelFactory(lobbyService)
}
