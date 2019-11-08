package tv.caffeine.app.lobby

import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.lobby.fragment.ClusterData
import tv.caffeine.app.lobby.type.ClusterType

interface LobbyItem {
    val id: String
    val itemType: Type

    enum class Type {
        AVATAR_CARD, FOLLOW_PEOPLE_CARD, HEADER, SUBTITLE, LIVE_BROADCAST_CARD, LIVE_BROADCAST_WITH_FRIENDS_CARD,
        PREVIOUS_BROADCAST_CARD, CARD_LIST, UPCOMING_BUTTON_CARD,
        SINGLE_CATEGORY_CARD, DOUBLE_CATEGORY_CARD, CATEGORY_CARD_LIST,
        EMPTY_JOIN_YOUR_FRIENDS
    }

    // An interface may not implement a method of 'Any'
    fun equals(other: LobbyItem) = id == other.id && itemType == other.itemType

    companion object {

        fun parse(result: Lobby, separateOffline: Boolean = false): List<LobbyItem> {
            val lobbyItems = mutableListOf<LobbyItem>()
            result.header.avatarCard?.let {
                lobbyItems.add(WelcomeCard("username", it.username))
            }
            result.header.followPeople?.displayMessage?.let {
                lobbyItems.add(FollowPeople(displayMessage = it))
            }
            return lobbyItems.plus(result.sections.flatMap { section ->
                mutableListOf<LobbyItem>().apply {
                    if (section.name != null) {
                        add(Header(section.name, section.name))
                    }
                    section.emptyMessage?.let { add(Subtitle(it, it)) }
                    section.broadcasters?.map(::convert)?.let { addAll(it) }
                    section.categories?.forEach { category ->
                        add(Subtitle(category.id, category.name))
                        if (!separateOffline) {
                            add(CardList(category.id + ".list", category.broadcasters.map(::convert)))
                        } else {
                            add(CardList(category.id + ".list", category.broadcasters.filter { it.broadcast != null }.map(::convert)))
                            addAll(category.broadcasters.filter { it.broadcast == null }.map(::convert))
                        }
                    }
                }.toList()
            })
        }

        private fun convert(broadcaster: Lobby.Broadcaster): SingleCard =
            when {
                broadcaster.broadcast == null -> PreviousBroadcast(broadcaster.id, broadcaster)
                broadcaster.followingViewersCount == 0 -> LiveBroadcast(broadcaster.id, broadcaster)
                else -> LiveBroadcastWithFriends(broadcaster.id, broadcaster)
            }

        fun parse(lobby: LobbyQuery.Data): List<LobbyItem> =
            parse(lobby.pagePayload.clusters.map { it.fragments.clusterData })

        fun parse(detailPage: DetailPageQuery.Data): List<LobbyItem> =
            parse(detailPage.pagePayload.clusters.map { it.fragments.clusterData })

        private fun parse(clusters: List<ClusterData>): List<LobbyItem> {
            val lobbyItems = mutableListOf<LobbyItem>()
            for (cluster in clusters) {
                if (cluster.type == ClusterType.EMPTY_JOIN_YOUR_FRIENDS) {
                    lobbyItems.add(EmptyJoinYourFriends(cluster.type.name))
                } else if (cluster.name != null) {
                    lobbyItems.add(Header(cluster.name, cluster.name))
                }
                cluster.cardLists.forEach { genericCardList ->
                    (genericCardList.inlineFragment as? ClusterData.AsLiveBroadcastCardList)?.let { cardList ->
                        val totalCount = cardList.liveBroadcastCards.size
                        val maxLargeCardDisplayCount = cardList.maxLargeCardDisplayCount ?: totalCount
                        cardList.liveBroadcastCards.take(maxLargeCardDisplayCount).forEach {
                            lobbyItems.add(it.toLiveCard())
                        }
                        if (totalCount > maxLargeCardDisplayCount) {
                            lobbyItems.add(CardList(
                                cardList.id,
                                cardList.liveBroadcastCards.subList(maxLargeCardDisplayCount, totalCount).map { it.toLiveCard() }
                            ))
                        }
                    }
                    (genericCardList.inlineFragment as? ClusterData.AsCreatorCardList)?.let { cardList ->
                        cardList.creatorCards.forEach {
                            lobbyItems.add(it.toOfflineCard())
                        }
                    }
                    (genericCardList.inlineFragment as? ClusterData.AsCategoryCardList)?.let { cardList ->
                        when (cardList.categoryCards.size) {
                            1 -> lobbyItems.add(SingleCategory(cardList.id, cardList.categoryCards[0]))
                            2 -> lobbyItems.add(DoubleCategory(cardList.id, cardList))
                            else -> lobbyItems.add(CategoryCardList(
                                cardList.id, cardList.categoryCards.map { SingleCategory(it.id, it) }))
                        }
                    }
                }
            }
            return lobbyItems
        }
    }
}

data class WelcomeCard(override val id: String, val username: String) : LobbyItem {
    override val itemType = LobbyItem.Type.AVATAR_CARD
}

data class FollowPeople(override val id: String = "followPeople", val displayMessage: String) : LobbyItem {
    override val itemType = LobbyItem.Type.FOLLOW_PEOPLE_CARD
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
data class LiveBroadcastWithFriends(override val id: String, override val broadcaster: Lobby.Broadcaster) : SingleCard() {
    override val itemType = LobbyItem.Type.LIVE_BROADCAST_WITH_FRIENDS_CARD
}
data class PreviousBroadcast(override val id: String, override val broadcaster: Lobby.Broadcaster) : SingleCard() {
    override val itemType = LobbyItem.Type.PREVIOUS_BROADCAST_CARD
}
data class CardList(override val id: String, val cards: List<SingleCard>) : LobbyItem {
    override val itemType = LobbyItem.Type.CARD_LIST
}
data class UpcomingButtonItem(override val id: String) : LobbyItem {
    override val itemType = LobbyItem.Type.UPCOMING_BUTTON_CARD
}
data class SingleCategory(override val id: String, val categoryCard: ClusterData.CategoryCard) : LobbyItem {
    override val itemType = LobbyItem.Type.SINGLE_CATEGORY_CARD
}
data class DoubleCategory(override val id: String, val categoryCardList: ClusterData.AsCategoryCardList) : LobbyItem {
    override val itemType = LobbyItem.Type.DOUBLE_CATEGORY_CARD
}
data class CategoryCardList(override val id: String, val categoryCards: List<SingleCategory>) : LobbyItem {
    override val itemType = LobbyItem.Type.CATEGORY_CARD_LIST
}
data class EmptyJoinYourFriends(override val id: String) : LobbyItem {
    override val itemType = LobbyItem.Type.EMPTY_JOIN_YOUR_FRIENDS
}
