package tv.caffeine.app.lobby

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.api.model.Lobby

@RunWith(RobolectricTestRunner::class)
class LobbyItemTests {

    @Test
    fun `no section header from the API`() {
        val sections = arrayOf<Lobby.Section>()
        val lobby = Lobby(mapOf(), mapOf(), Lobby.Header(), sections)
        val lobbyItems = LobbyItem.parse(lobby)
        for (lobbyItem in lobbyItems) {
            assertFalse("No section header should be shown.", lobbyItem is Header)
        }
    }

    @Test
    fun `do not show an empty section header`() {
        val sectionName = null
        val sections = arrayOf(Lobby.Section("0", "SimpleCardList", sectionName, null, null, null))
        val lobby = Lobby(mapOf(), mapOf(), Lobby.Header(), sections)
        val lobbyItems = LobbyItem.parse(lobby)
        assertNull("The empty section header should not be shown.", lobbyItems.find { it is Header })
    }

    @Test
    fun `show a non-empty section header`() {
        val sectionName = "Suggested For You"
        val sections = arrayOf(Lobby.Section("0", "SimpleCardList", sectionName, null, null, null))
        val lobby = Lobby(mapOf(), mapOf(), Lobby.Header(), sections)
        val lobbyItems = LobbyItem.parse(lobby)
        val sectionHeader = lobbyItems.find { it is Header } as? Header
        assertNotNull("The non-empty section header should be shown.", sectionHeader)
        assertEquals(sectionHeader?.text, sectionName)
    }
}

