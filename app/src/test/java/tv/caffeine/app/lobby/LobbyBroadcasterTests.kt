package tv.caffeine.app.lobby

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.api.model.Broadcast
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.api.model.User

@RunWith(RobolectricTestRunner::class)
class LobbyBroadcasterTests {

    private val usernames1 = listOf("a", "b", "c", "offline1")
    private val usernames2 = listOf("d", "e", "f", "offline2")
    private val offlineUsernames = listOf("offline1", "offline2")

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `include all live broadcasters at the top of the lobby in the stage pager`() {
        val lobbySections = arrayOf(getLobbySectionExample(usernames1), getLobbySectionExample(usernames2))
        val broadcasters = getLobbyExample(lobbySections).getAllBroadcasters()
        assertEquals(listOf("a", "b", "c", "d", "e", "f"), broadcasters)
    }

    @Test
    fun `include all live broadcasters from the categories of the lobby in the stage pager`() {
        val lobbySections = arrayOf(getLobbyCategoryExample(usernames1), getLobbyCategoryExample(usernames2))
        val broadcasters = getLobbyExample(lobbySections).getAllBroadcasters()
        assertEquals(listOf("a", "b", "c", "d", "e", "f"), broadcasters)
    }

    @Test
    fun `include all live broadcasters from the top section and the categories of the lobby in the stage pager`() {
        val lobbySections = arrayOf(getLobbySectionExample(usernames1), getLobbyCategoryExample(usernames2))
        val broadcasters = getLobbyExample(lobbySections).getAllBroadcasters()
        assertEquals(listOf("a", "b", "c", "d", "e", "f"), broadcasters)
    }

    private fun getLobbyExample(sections: Array<Lobby.Section>) =
            Lobby("0", mapOf(), mapOf(), Lobby.Header(null, null), sections)

    private fun getLobbySectionExample(usernames: List<String>) =
            Lobby.Section("0", "0", "0", "0", getBroadcasterExamples(usernames), null)

    private fun getLobbyCategoryExample(usernames: List<String>) =
            Lobby.Section("0", "0", "0", "0", null, arrayOf(getCategoryExample(usernames)))

    private fun getCategoryExample(usernames: List<String>) =
            Lobby.Category("0", "0", getBroadcasterExamples(usernames))

    private fun getBroadcasterExamples(usernames: List<String>): Array<Lobby.Broadcaster> {
        return usernames.map {
            Lobby.Broadcaster("0", "0", getUserExample(it), "0",
                if (it in offlineUsernames) null else mockk<Broadcast>(), null, listOf(), 0, 10, null)
        }.toTypedArray()
    }

    private fun getUserExample(username: String): User {
        val user: User = mockk()
        every { user.username } returns username
        return user
    }
}
