package tv.caffeine.app.lobby

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.api.model.User
import tv.caffeine.app.util.makeGenericUser

@RunWith(RobolectricTestRunner::class)
class FriendsWatchingFormatterTests {
    private val unverifiedUser = User("caidA", "usernameA", "name", "email", "/avatarImagePath", 0, 0, false, false, "broadcastId", "stageId", mapOf(), mapOf(), 21, "bio", "countryCode", "countryName", "gender", false, false, null, null, false)
    private val verifiedUser = User("caidA", "usernameA", "name", "email", "/avatarImagePath", 0, 0, true, false, "broadcastId", "stageId", mapOf(), mapOf(), 21, "bio", "countryCode", "countryName", "gender", false, false, null, null, false)
    private val casterUser = User("caidA", "usernameA", "name", "email", "/avatarImagePath", 0, 0, false, true, "broadcastId", "stageId", mapOf(), mapOf(), 21, "bio", "countryCode", "countryName", "gender", false, false, null, null, false)
    private val verifiedCasterUser = User("caidA", "usernameA", "name", "email", "/avatarImagePath", 0, 0, true, true, "broadcastId", "stageId", mapOf(), mapOf(), 21, "bio", "countryCode", "countryName", "gender", false, false, null, null, false)

    @Test
    fun `when no friend is watching, return null`() {
        val user = makeGenericUser()
        val context = InstrumentationRegistry.getInstrumentation().context
        val broadcaster = Lobby.Broadcaster("fake id", "Featured", user, "tag", null, null, listOf(), 0, 10, null)
        val string = formatFriendsWatchingString(context, broadcaster)
        assertNull(string)
        val shortString = formatFriendsWatchingShortString(context, broadcaster)
        assertNull(shortString)
    }

    @Test
    fun `when 1 unverified friend is watching, show their avatar and name`() {
        val user = makeGenericUser()
        val friendA = unverifiedUser
        val context = InstrumentationRegistry.getInstrumentation().context
        val broadcaster = Lobby.Broadcaster("fake id", "Featured", user, "tag", null, null, listOf(friendA), 1, 10, null)
        val string = formatFriendsWatchingString(context, broadcaster)
        assertEquals("<img src=\"https://images.caffeine.tv/avatarImagePath\"> usernameA is watching", string)
        val shortString = formatFriendsWatchingShortString(context, broadcaster)
        assertEquals("<img src=\"https://images.caffeine.tv/avatarImagePath\"> usernameA", shortString)
    }

    @Test
    fun `when 1 verified friend is watching, show their avatar, name and verified badge`() {
        val user = makeGenericUser()
        val friendA = verifiedUser
        val context = InstrumentationRegistry.getInstrumentation().context
        val broadcaster = Lobby.Broadcaster("fake id", "Featured", user, "tag", null, null, listOf(friendA), 1, 10, null)
        val string = formatFriendsWatchingString(context, broadcaster)
        assertEquals("<img src=\"https://images.caffeine.tv/avatarImagePath\"> <b>usernameA</b> <img src=\"verified_white\"/> is watching", string)
        val shortString = formatFriendsWatchingShortString(context, broadcaster)
        assertEquals("<img src=\"https://images.caffeine.tv/avatarImagePath\"> <b>usernameA</b> <img src=\"verified_white\"/>", shortString)
    }

    @Test
    fun `when 1 unverified caster friend is watching, show their avatar, name and caster badge`() {
        val user = makeGenericUser()
        val friendA = casterUser
        val context = InstrumentationRegistry.getInstrumentation().context
        val broadcaster = Lobby.Broadcaster("fake id", "Featured", user, "tag", null, null, listOf(friendA), 1, 10, null)
        val string = formatFriendsWatchingString(context, broadcaster)
        assertEquals("<img src=\"https://images.caffeine.tv/avatarImagePath\"> <b>usernameA</b> <img src=\"caster\"/> is watching", string)
        val shortString = formatFriendsWatchingShortString(context, broadcaster)
        assertEquals("<img src=\"https://images.caffeine.tv/avatarImagePath\"> <b>usernameA</b> <img src=\"caster\"/>", shortString)
    }

    @Test
    fun `when 1 verified caster friend is watching, show their avatar, name and verified badge`() {
        val user = makeGenericUser()
        val friendA = verifiedCasterUser
        val context = InstrumentationRegistry.getInstrumentation().context
        val broadcaster = Lobby.Broadcaster("fake id", "Featured", user, "tag", null, null, listOf(friendA), 1, 10, null)
        val string = formatFriendsWatchingString(context, broadcaster)
        assertEquals("<img src=\"https://images.caffeine.tv/avatarImagePath\"> <b>usernameA</b> <img src=\"verified_white\"/> is watching", string)
        val shortString = formatFriendsWatchingShortString(context, broadcaster)
        assertEquals("<img src=\"https://images.caffeine.tv/avatarImagePath\"> <b>usernameA</b> <img src=\"verified_white\"/>", shortString)
    }

    @Test
    fun `when 2 friends are watching, show the first friend's avatar, name and maybe badge and correct quantity`() {
        val user = makeGenericUser()
        val friendA = verifiedCasterUser
        val friendB = casterUser
        val context = InstrumentationRegistry.getInstrumentation().context
        val followingViewers = listOf(friendA, friendB)
        val broadcaster = Lobby.Broadcaster("fake id", "Featured", user, "tag", null, null,
            followingViewers, followingViewers.size, 10, null)
        val string = formatFriendsWatchingString(context, broadcaster)
        assertEquals("<img src=\"https://images.caffeine.tv/avatarImagePath\"> <b>usernameA</b> <img src=\"verified_white\"/> and one other are watching", string)
        val shortString = formatFriendsWatchingShortString(context, broadcaster)
        assertEquals("<img src=\"https://images.caffeine.tv/avatarImagePath\"> <b>usernameA</b> <img src=\"verified_white\"/> + 1", shortString)
    }

    @Test
    fun `when 3 friends are watching, show the first friend's avatar, name and maybe badge and correct quantity`() {
        val user = makeGenericUser()
        val friendA = verifiedCasterUser
        val friendB = casterUser
        val friendC = verifiedUser
        val context = InstrumentationRegistry.getInstrumentation().context
        val followingViewers = listOf(friendA, friendB, friendC)
        val broadcaster = Lobby.Broadcaster("fake id", "Featured", user, "tag", null, null,
            followingViewers, followingViewers.size, 10, null)
        val string = formatFriendsWatchingString(context, broadcaster)
        assertEquals("<img src=\"https://images.caffeine.tv/avatarImagePath\"> <b>usernameA</b> <img src=\"verified_white\"/> and 2 others are watching", string)
        val shortString = formatFriendsWatchingShortString(context, broadcaster)
        assertEquals("<img src=\"https://images.caffeine.tv/avatarImagePath\"> <b>usernameA</b> <img src=\"verified_white\"/> + 2", shortString)
    }
}
