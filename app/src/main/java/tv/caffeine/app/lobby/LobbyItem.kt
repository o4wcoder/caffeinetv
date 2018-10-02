package tv.caffeine.app.lobby

import tv.caffeine.app.api.model.Lobby

interface LobbyItem {
    val id: String
    val itemType: Type

    enum class Type {
        HEADER, SUBTITLE, LIVE_BROADCAST_CARD, PREVIOUS_BROADCAST_CARD, CARD_LIST
    }

    companion object {

        fun parse(result: Lobby.Result): List<LobbyItem> {
            return result.sections.flatMap { section ->
                mutableListOf<LobbyItem>(Header(section.id, section.name)).apply {
                    section.emptyMessage?.let { add(Subtitle(section.id + ".msg", it)) }
                    section.broadcasters?.map(::convert)?.let { addAll(it) }
                    section.categories?.forEach { category ->
                        add(Subtitle(category.id, category.name))
                        add(CardList(category.id + ".list", category.broadcasters.map(::convert)))
                    }
                }.toList()
            }
        }

        private fun convert(broadcaster: Lobby.Broadcaster): SingleCard =
                if (broadcaster.broadcast != null) {
                    LiveBroadcast(broadcaster.id, broadcaster)
                } else {
                    PreviousBroadcast(broadcaster.id, broadcaster)
                }
    }

}

data class Header(override val id: String, val text: String) : LobbyItem {
    override val itemType = LobbyItem.Type.HEADER
}
data class Subtitle(override val id: String, val text: String) : LobbyItem {
    override val itemType = LobbyItem.Type.SUBTITLE
}
abstract class SingleCard : LobbyItem {
    abstract val broadcaster: Lobby.Broadcaster
}
data class LiveBroadcast(override val id: String, override val broadcaster: Lobby.Broadcaster) : SingleCard() {
    override val itemType = LobbyItem.Type.LIVE_BROADCAST_CARD
}
data class PreviousBroadcast(override val id: String, override val broadcaster: Lobby.Broadcaster) : SingleCard() {
    override val itemType = LobbyItem.Type.PREVIOUS_BROADCAST_CARD
}
data class CardList(override val id: String, val cards: List<SingleCard>) : LobbyItem {
    override val itemType = LobbyItem.Type.CARD_LIST
}
