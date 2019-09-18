package tv.caffeine.app.lobby

import tv.caffeine.app.lobby.release.ReleaseLobbyAdapter
import tv.caffeine.app.lobby.type.Page
import javax.inject.Inject
import javax.inject.Provider

class HomeLobbyFragment @Inject constructor(
    releaseLobbyAdapterFactoryProvider: Provider<ReleaseLobbyAdapter.Factory>
) : LobbyV5Fragment(releaseLobbyAdapterFactoryProvider) {

    override fun getPage() = Page.HOME
}
