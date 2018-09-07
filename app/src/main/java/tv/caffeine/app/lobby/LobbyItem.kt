package tv.caffeine.app.lobby

import tv.caffeine.app.api.Api

sealed class LobbyItem {
    class Header(val text: String) : LobbyItem()
    class Subtitle(val text: String) : LobbyItem()
    abstract class SingleCard(val broadcaster: Api.v3.Lobby.Broadcaster) : LobbyItem()
    class LiveBroadcast(broadcaster: Api.v3.Lobby.Broadcaster) : SingleCard(broadcaster)
    class PreviousBroadcast(broadcaster: Api.v3.Lobby.Broadcaster) : SingleCard(broadcaster)
//    class SingleCardV2(val title: String, val subtitle: String, val isLive: Boolean, val isFollowed: Boolean, val isFeatured: Boolean, val previewImageUrl: String, val avatarImageUrl: String, val contentLogoImageUrl: String) : LobbyItem()
    class CardList(val cards: List<SingleCard>) : LobbyItem()

    companion object {
        const val HEADER = 1
        const val SUBTITLE = 2
        const val LIVE_BROADCAST_CARD = 3
        const val PREVIOUS_BROADCAST_CARD = 4
        const val CARD_LIST = 5

        fun parse(result: Api.v3.Lobby.Result): List<LobbyItem> {
            return result.sections.flatMap { section ->
                mutableListOf<LobbyItem>(Header(section.name)).apply {
                    section.emptyMessage?.let { add(Subtitle(it)) }
                    section.broadcasters?.map(::convert)?.let { addAll(it) }
                    section.categories?.forEach { category ->
                        add(Subtitle(category.name))
                        add(CardList(category.broadcasters.map(::convert)))
                    }
                }.toList()
            }
        }

        private fun convert(broadcaster: Api.v3.Lobby.Broadcaster): SingleCard =
                if (broadcaster.broadcast != null) {
                    LiveBroadcast(broadcaster)
                } else {
                    PreviousBroadcast(broadcaster)
                }
    }

    fun itemType(): Int = when(this) {
        is LobbyItem.Header -> HEADER
        is LobbyItem.Subtitle -> SUBTITLE
        is LobbyItem.LiveBroadcast -> LIVE_BROADCAST_CARD
        is LobbyItem.PreviousBroadcast -> PREVIOUS_BROADCAST_CARD
        is LobbyItem.CardList -> CARD_LIST
        is LobbyItem.SingleCard -> error("Unexpected item")
    }
}