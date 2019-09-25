package tv.caffeine.app.lobby

import androidx.navigation.fragment.navArgs
import tv.caffeine.app.lobby.release.ReleaseLobbyAdapter
import javax.inject.Inject
import javax.inject.Provider

class LobbyDetailFragment @Inject constructor(
    releaseLobbyAdapterFactoryProvider: Provider<ReleaseLobbyAdapter.Factory>
) : LobbyV5Fragment(releaseLobbyAdapterFactoryProvider) {

    private val args by navArgs<LobbyDetailFragmentArgs>()

    override fun loadLobby() {
        viewModel.refreshDetail(args.cardId)
    }
}
