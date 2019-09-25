package tv.caffeine.app.lobby

import org.junit.Assert.assertEquals
import org.junit.Test
import tv.caffeine.app.lobby.fragment.ClusterData
import tv.caffeine.app.lobby.fragment.UserData
import tv.caffeine.app.lobby.type.AgeRestriction

class GraphqlLobbyExtTests {

    @Test
    fun `the live card from the v5 API can be turned to a live card`() {
        val graphqlUser = ClusterData.User(
            "_typename",
            ClusterData.User.Fragments(getUserFragment("caid1", "user1"))
        )
        val friendViewers = ClusterData.FriendViewer(
            "_typename",
            ClusterData.FriendViewer.Fragments(getUserFragment("caid2", "friend2"))
        )
        val graphqlBroadcast = ClusterData.Broadcast(
            "_typename",
            "caid1",
            "broadcast_name",
            "top pick",
            "/preview_image_path/image.jpg",
            "/game_image_path/image.jpg",
            AgeRestriction.SEVENTEEN_PLUS,
            "content_id",
            listOf(friendViewers),
            1
        )
        val graphqlLiveBroadcastCard = ClusterData.LiveBroadcastCard(
            "_typename",
            "cluster_id",
            "Top pick",
            2,
            graphqlUser,
            graphqlBroadcast
        )

        val liveCard = graphqlLiveBroadcastCard.toLiveCard()
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
    fun `the creator card from the v5 API can be turned to an offline card`() {
        val graphqlUser = ClusterData.User1(
            "_typename",
            ClusterData.User1.Fragments(getUserFragment("caid1", "user1"))
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
        val userFragment1 = getUserFragment("caid1", "user1", true, true, true)
        val userFragment2 = getUserFragment("caid2", "user2", false, false, false)
        val user1 = userFragment1.toCaffeineUser()
        val user2 = userFragment2.toCaffeineUser()

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

    private fun getUserFragment(
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