package tv.caffeine.app.api.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelExtTests {

    @Test
    fun `conversion from online broadcaster to lobby impression`() {
        val subject = Lobby.Broadcaster("id", "type", genericUser, "tagId", broadcast, null,
            listOf(), 0, 0, "clusterId")

        val lobbyImpressionEventData = subject.makeLobbyImpressionEventData("payloadId", 123000L)
        assertEquals("caid", lobbyImpressionEventData.caid)
        assertEquals("clusterId", lobbyImpressionEventData.clusterId)
        assertEquals(true, lobbyImpressionEventData.isLive)
    }

    @Test
    fun `conversion from offline broadcaster to lobby impression`() {
        val subject = Lobby.Broadcaster("id", "type", genericUser, "tagId", null, null,
            listOf(), 0, 0, "clusterId")

        val lobbyImpressionEventData = subject.makeLobbyImpressionEventData("payloadId", 123000L)
        assertEquals("caid", lobbyImpressionEventData.caid)
        assertEquals("clusterId", lobbyImpressionEventData.clusterId)
        assertEquals(false, lobbyImpressionEventData.isLive)
    }

    private val genericUser = User(
        "caid",
        "username",
        "name",
        "email",
        "/avatarImagePath",
        0,
        0,
        false,
        false,
        "broadcastId",
        "stageId",
        mapOf(),
        mapOf(),
        21,
        "bio",
        "countryCode",
        "countryName",
        "gender",
        false,
        false,
        null,
        null,
        false
    )

    private val broadcast = Broadcast(
        "broadcastId",
        "name",
        "contentId",
        null,
        null,
        "imagePath",
        Broadcast.State.ONLINE,
        "dateText",
        null,
        null
    )
}
