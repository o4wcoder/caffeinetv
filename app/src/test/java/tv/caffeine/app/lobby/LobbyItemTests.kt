package tv.caffeine.app.lobby

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.lobby.fragment.ClusterData
import tv.caffeine.app.lobby.fragment.UserData
import tv.caffeine.app.lobby.type.AgeRestriction

@RunWith(RobolectricTestRunner::class)
class LobbyItemTests {

    @Test
    fun `no section header from the API`() {
        val sections = arrayOf<Lobby.Section>()
        val lobby = Lobby("0", mapOf(), mapOf(), Lobby.Header(), sections)
        val lobbyItems = LobbyItem.parse(lobby)
        for (lobbyItem in lobbyItems) {
            assertFalse("No section header should be shown.", lobbyItem is Header)
        }
    }

    @Test
    fun `do not show an empty section header`() {
        val sectionName = null
        val sections = arrayOf(Lobby.Section("0", "SimpleCardList", sectionName, null, null, null))
        val lobby = Lobby("0", mapOf(), mapOf(), Lobby.Header(), sections)
        val lobbyItems = LobbyItem.parse(lobby)
        assertNull("The empty section header should not be shown.", lobbyItems.find { it is Header })
    }

    @Test
    fun `show a non-empty section header`() {
        val sectionName = "Suggested For You"
        val sections = arrayOf(Lobby.Section("0", "SimpleCardList", sectionName, null, null, null))
        val lobby = Lobby("0", mapOf(), mapOf(), Lobby.Header(), sections)
        val lobbyItems = LobbyItem.parse(lobby)
        val sectionHeader = lobbyItems.find { it is Header } as? Header
        assertNotNull("The non-empty section header should be shown.", sectionHeader)
        assertEquals(sectionHeader?.text, sectionName)
    }

    @Test
    fun `show the welcome avatar card`() {
        val username = "test username"
        val header = Lobby.Header(Lobby.MiniUser(username))
        val lobby = Lobby("0", mapOf(), mapOf(), header, arrayOf())
        val lobbyItems = LobbyItem.parse(lobby)
        val welcomeCard = lobbyItems.find { it is WelcomeCard } as? WelcomeCard
        assertNotNull("The welcome card that prompts to upload the avatar should be shown", welcomeCard)
        assertEquals(welcomeCard?.username, username)
    }

    @Test
    fun `show the follow people card`() {
        val displayMessage = "When people you follow are watching or broadcasting you’ll see them here."
        val header = Lobby.Header(followPeople = Lobby.FollowPeoplePrompt(displayMessage))
        val lobby = Lobby("0", mapOf(), mapOf(), header, arrayOf())
        val lobbyItems = LobbyItem.parse(lobby)
        val followPeopleCard = lobbyItems.find { it is FollowPeople } as? FollowPeople
        assertNotNull("The follow people card should be shown", followPeopleCard)
        assertEquals(followPeopleCard?.displayMessage, displayMessage)
    }

    @Test
    fun `the special cards are in order`() {
        val errorIndex = -1
        val username = "test username"
        val displayMessage = "When people you follow are watching or broadcasting you’ll see them here."
        val header = Lobby.Header(Lobby.MiniUser(username), Lobby.FollowPeoplePrompt(displayMessage))
        val lobby = Lobby("0", mapOf(), mapOf(), header, arrayOf())
        val lobbyItems = LobbyItem.parse(lobby)

        val welcomeCard = lobbyItems.find { it is WelcomeCard } as? WelcomeCard
        val followPeopleCard = lobbyItems.find { it is FollowPeople } as? FollowPeople
        val welcomeCardIndex = welcomeCard?.let { lobbyItems.indexOf(it) } ?: errorIndex
        val followPeopleCardIndex = followPeopleCard?.let { lobbyItems.indexOf(it) } ?: errorIndex
        assertTrue("Both the welcome avatar card and the follow people card should show up",
                welcomeCardIndex != errorIndex && followPeopleCardIndex != errorIndex)
        assertTrue("The welcome avatar card should show before the follow people card",
                welcomeCardIndex < followPeopleCardIndex)
    }

    @Test
    fun `do not show the card list if maxLargeCountDisplayCount is null`() {
        val cards = (1..3).map { buildLiveCard(it) }
        val data = buildLobbyV5WithLiveCards(cards, null)
        val lobbyItems = LobbyItem.parse(data)
        assertTrue(lobbyItems[0] is Header)
        assertTrue(lobbyItems[1] is SingleCard)
        assertTrue(lobbyItems[2] is SingleCard)
        assertTrue(lobbyItems[3] is SingleCard)
    }

    @Test
    fun `do not show the card list if maxLargeCountDisplayCount is larger than the card count`() {
        val cards = (1..3).map { buildLiveCard(it) }
        val data = buildLobbyV5WithLiveCards(cards, 5)
        val lobbyItems = LobbyItem.parse(data)
        assertTrue(lobbyItems[0] is Header)
        assertTrue(lobbyItems[1] is SingleCard)
        assertTrue(lobbyItems[2] is SingleCard)
        assertTrue(lobbyItems[3] is SingleCard)
    }

    @Test
    fun `show large cards and then the card list if maxLargeCountDisplayCount is smaller than the card count`() {
        val cards = (1..3).map { buildLiveCard(it) }
        val data = buildLobbyV5WithLiveCards(cards, 2)
        val lobbyItems = LobbyItem.parse(data)
        assertTrue(lobbyItems[0] is Header)
        assertTrue(lobbyItems[1] is SingleCard)
        assertTrue(lobbyItems[2] is SingleCard)
        assertTrue(lobbyItems[3] is CardList)
        assertEquals(1, (lobbyItems[3] as CardList).cards.size)
    }

    @Test
    fun `show a single category card if the card count is 1`() {
        val cards = (1..1).map { buildCategoryCard(it) }
        val data = buildLobbyV5WithCategoryCards(cards)
        val lobbyItems = LobbyItem.parse(data)
        assertTrue(lobbyItems[0] is Header)
        assertTrue(lobbyItems[1] is SingleCategory)
    }

    @Test
    fun `show a double category card if the card count is 2`() {
        val cards = (1..2).map { buildCategoryCard(it) }
        val data = buildLobbyV5WithCategoryCards(cards)
        val lobbyItems = LobbyItem.parse(data)
        assertTrue(lobbyItems[0] is Header)
        assertTrue(lobbyItems[1] is DoubleCategory)
    }

    private fun buildLobbyV5WithLiveCards(
        cards: List<ClusterData.LiveBroadcastCard>,
        maxLargeCardDisplayCount: Int?
    ) = LobbyQuery.Data(
        LobbyQuery.PagePayload(
            "", listOf(
                LobbyQuery.Cluster(
                    "", LobbyQuery.Cluster.Fragments(
                        ClusterData(
                            "", "name", listOf(
                                ClusterData.CardList(
                                    "", ClusterData.AsLiveBroadcastCardList(
                                        "", "cardListId", maxLargeCardDisplayCount, cards
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    private fun buildLobbyV5WithCategoryCards(
        cards: List<ClusterData.CategoryCard>
    ) = LobbyQuery.Data(
        LobbyQuery.PagePayload(
            "", listOf(
                LobbyQuery.Cluster(
                    "", LobbyQuery.Cluster.Fragments(
                        ClusterData(
                            "", "name", listOf(
                                ClusterData.CardList("", ClusterData.AsCategoryCardList(
                                    "", "cardListId", cards
                                ))
                            )
                        )
                    )
                )
            )
        )
    )

    private fun buildLiveCard(index: Int, name: String? = null): ClusterData.LiveBroadcastCard {
        val userFragment = UserData(
            "",
            "caid$index",
            "username$index",
            "",
            false,
            false,
            false
        )
        val broadcast = ClusterData.Broadcast(
            "",
            "id$index",
            "name$index",
            "description$index",
            "/previewImage.jpg",
            "/gameImage.jpg",
            AgeRestriction.SEVENTEEN_PLUS,
            "contentId$index",
            listOf(),
            0
        )
        return ClusterData.LiveBroadcastCard(
            "",
            index.toString(),
            name,
            index,
            ClusterData.User("", ClusterData.User.Fragments(userFragment)),
            broadcast
        )
    }

    private fun buildCategoryCard(index: Int) = ClusterData.CategoryCard(
        "",
        index.toString(),
        index,
        "name$index",
        "/backgroundImage.jpg",
        "/overlayImage.jpg"
    )
}
