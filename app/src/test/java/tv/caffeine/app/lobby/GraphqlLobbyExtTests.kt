package tv.caffeine.app.lobby

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tv.caffeine.app.lobby.fragment.ClusterData
import tv.caffeine.app.lobby.fragment.UserData
import tv.caffeine.app.lobby.type.AgeRestriction

class GraphqlLobbyExtTests {

    @Test
    fun `the live card from the v5 API can be turned to a live card`() {
        val liveCard = buildLiveBroadcastCard().toLiveCard()
        val broadcaster = liveCard.broadcaster
        assertEquals("caid1", liveCard.id)
        assertEquals("user1", broadcaster.user.username)
        assertEquals("broadcast_name", broadcaster.broadcast?.name)
        assertEquals("https://images.caffeine.tv/preview_image_path/image.jpg", broadcaster.broadcast?.previewImageUrl)
        assertEquals("/game_image_path/image.jpg", broadcaster.broadcast?.game?.iconImagePath)
        assertEquals(null, broadcaster.lastBroadcast)
        assertEquals("friend2", broadcaster.followingViewers?.get(0)?.username)
        assertEquals(1, broadcaster.followingViewersCount)
        assertEquals(2, broadcaster.displayOrder)
        assertEquals("cluster_id", broadcaster.clusterId)
        assertEquals("Top pick", broadcaster.badgeText)
    }

    @Test
    fun `the age restriction badge is 17+ if the live card has an age restriction of 17+`() {
        val liveCard = buildLiveBroadcastCard(AgeRestriction.SEVENTEEN_PLUS).toLiveCard()
        val broadcaster = liveCard.broadcaster
        assertEquals("17+", broadcaster.ageRestriction)
    }

    @Test
    fun `the age restriction badge is null if the live card does not have an age restriction of 17+`() {
        val liveCard = buildLiveBroadcastCard(null).toLiveCard()
        val broadcaster = liveCard.broadcaster
        assertNull(broadcaster.ageRestriction)
    }

    private fun buildLiveBroadcastCard(ageRestriction: AgeRestriction? = null): ClusterData.LiveBroadcastCard {
        val graphqlUser = ClusterData.User(
            "_typename",
            ClusterData.User.Fragments(getUserData("caid1", "user1"))
        )
        val friendViewers = ClusterData.FriendViewer(
            "_typename",
            ClusterData.FriendViewer.Fragments(getUserData("caid2", "friend2"))
        )
        val graphqlBroadcast = ClusterData.Broadcast(
            "_typename",
            "caid1",
            "broadcast_name",
            "top pick",
            "/preview_image_path/image.jpg",
            "/game_image_path/image.jpg",
            ageRestriction,
            "content_id",
            listOf(friendViewers),
            1
        )
        return ClusterData.LiveBroadcastCard(
            "_typename",
            "cluster_id",
            "Top pick",
            2,
            graphqlUser,
            graphqlBroadcast
        )
    }

    @Test
    fun `the creator card from the v5 API can be turned to an offline card`() {
        val graphqlUser = ClusterData.User1(
            "_typename",
            ClusterData.User1.Fragments(getUserData("caid1", "user1"))
        )
        val graphqlCreatorCard = ClusterData.CreatorCard(
            "_typename",
            "cluster_id",
            2,
            graphqlUser
        )

        val offlineCard = graphqlCreatorCard.toOfflineCard()
        val broadcaster = offlineCard.broadcaster
        assertEquals("caid1", offlineCard.id)
        assertEquals("user1", broadcaster.user.username)
        assertEquals(null, broadcaster.broadcast)
        assertEquals(null, broadcaster.followingViewers)
        assertEquals(0, broadcaster.followingViewersCount)
        assertEquals(2, broadcaster.displayOrder)
        assertEquals("cluster_id", broadcaster.clusterId)
    }

    @Test
    fun `the user from the v5 API can be turned to a Caffeine user in the lobby`() {
        val userData1 = getUserData("caid1", "user1", true, true, true)
        val userData2 = getUserData("caid2", "user2", false, false, false)
        val user1 = userData1.toCaffeineUser()
        val user2 = userData2.toCaffeineUser()

        assertEquals("caid1", user1.caid)
        assertEquals("user1", user1.username)
        assertEquals("https://images.caffeine.tv/avatar_image_path/user1.jpg", user1.avatarImageUrl)
        assertEquals(true, user1.isCaster)
        assertEquals(true, user1.isVerified)

        assertEquals("caid2", user2.caid)
        assertEquals("user2", user2.username)
        assertEquals("https://images.caffeine.tv/avatar_image_path/user2.jpg", user2.avatarImageUrl)
        assertEquals(false, user2.isCaster)
        assertEquals(false, user2.isVerified)
    }

    private fun getUserData(
        caid: String,
        username: String,
        isFollowing: Boolean = false,
        isCaster: Boolean = false,
        isVerified: Boolean = false
    ) = UserData(
        "_typename",
        caid,
        username,
        "/avatar_image_path/$username.jpg",
        isFollowing,
        isCaster,
        isVerified
    )
}