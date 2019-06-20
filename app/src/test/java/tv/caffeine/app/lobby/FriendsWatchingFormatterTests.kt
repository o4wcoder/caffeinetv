package tv.caffeine.app.lobby

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.api.model.User

@RunWith(RobolectricTestRunner::class)
class FriendsWatchingFormatterTests {
    private val genericUser = User("caid", "username", "name", "email", "/avatarImagePath", 0, 0, false, false, "broadcastId", "stageId", mapOf(), mapOf(), 21, "bio", "countryCode", "countryName", "gender", false, false, null, null, false)
    private val unverifiedUser = User("caidA", "usernameA", "name", "email", "/avatarImagePath", 0, 0, false, false, "broadcastId", "stageId", mapOf(), mapOf(), 21, "bio", "countryCode", "countryName", "gender", false, false, null, null, false)
    private val verifiedUser = User("caidA", "usernameA", "name", "email", "/avatarImagePath", 0, 0, true, false, "broadcastId", "stageId", mapOf(), mapOf(), 21, "bio", "countryCode", "countryName", "gender", false, false, null, null, false)
    private val casterUser = User("caidA", "usernameA", "name", "email", "/avatarImagePath", 0, 0, false, true, "broadcastId", "stageId", mapOf(), mapOf(), 21, "bio", "countryCode", "countryName", "gender", false, false, null, null, false)
    private val verifiedCasterUser = User("caidA", "usernameA", "name", "email", "/avatarImagePath", 0, 0, true, true, "broadcastId", "stageId", mapOf(), mapOf(), 21, "bio", "countryCode", "countryName", "gender", false, false, null, null, false)

    @Test
    fun `when no friend is watching, return null`() {
        val user = genericUser
        val context = InstrumentationRegistry.getInstrumentation().context
        val broadcaster = Lobby.Broadcaster("fake id", "Featured", user, "tag", null, null, listOf(), 0, 10, null)
        val string = formatFriendsWatchingString(context, broadcaster)
        assertNull(string)
    }

    @Test
    fun `when 1 unverified friend is watching, show their avatar and name`() {
        val user = genericUser
        val friendA = unverifiedUser
        val context = InstrumentationRegistry.getInstrumentation().context
        val broadcaster = Lobby.Broadcaster("fake id", "Featured", user, "tag", null, null, listOf(friendA), 1, 10, null)
        val string = formatFriendsWatchingString(context, broadcaster)
        assertEquals("<img src=\"https://images.caffeine.tv/avatarImagePath\"> usernameA is watching", string)
    }

    @Test
    fun `when 1 verified friend is watching, show their avatar, name and verified badge`() {
        val user = genericUser
        val friendA = verifiedUser
        val context = InstrumentationRegistry.getInstrumentation().context
        val broadcaster = Lobby.Broadcaster("fake id", "Featured", user, "tag", null, null, listOf(friendA), 1, 10, null)
        val string = formatFriendsWatchingString(context, broadcaster)
        assertEquals("<img src=\"https://images.caffeine.tv/avatarImagePath\"> <b>usernameA</b> <img src=\"verified_white\"/> is watching", string)
    }

    @Test
    fun `when 1 unverified caster friend is watching, show their avatar, name and caster badge`() {
        val user = genericUser
        val friendA = casterUser
        val context = InstrumentationRegistry.getInstrumentation().context
        val broadcaster = Lobby.Broadcaster("fake id", "Featured", user, "tag", null, null, listOf(friendA), 1, 10, null)
        val string = formatFriendsWatchingString(context, broadcaster)
        assertEquals("<img src=\"https://images.caffeine.tv/avatarImagePath\"> <b>usernameA</b> <img src=\"caster\"/> is watching", string)
    }

    @Test
    fun `when 1 verified caster friend is watching, show their avatar, name and verified badge`() {
        val user = genericUser
        val friendA = verifiedCasterUser
        val context = InstrumentationRegistry.getInstrumentation().context
        val broadcaster = Lobby.Broadcaster("fake id", "Featured", user, "tag", null, null, listOf(friendA), 1, 10, null)
        val string = formatFriendsWatchingString(context, broadcaster)
        assertEquals("<img src=\"https://images.caffeine.tv/avatarImagePath\"> <b>usernameA</b> <img src=\"verified_white\"/> is watching", string)
    }
}
