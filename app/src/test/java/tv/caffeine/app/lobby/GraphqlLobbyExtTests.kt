package tv.caffeine.app.lobby

import org.junit.Assert.assertEquals
import org.junit.Test
import tv.caffeine.app.lobby.fragment.UserFragment
import tv.caffeine.app.lobby.type.AgeRestriction

class GraphqlLobbyExtTests {

    @Test
    fun `the live card from the v5 API can be turned to a live card`() {
        val graphqlUser = LobbyQuery.User(
            "_typename",
            LobbyQuery.User.Fragments(getUserFragment("caid1", "user1"))
        )
        val friendViewers = LobbyQuery.FriendViewer(
            "_typename",
            LobbyQuery.FriendViewer.Fragments(getUserFragment("caid2", "friend2"))
        )
        val graphqlBroadcast = LobbyQuery.Broadcast(
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
        val graphqlLiveBroadcastCard = LobbyQuery.AsLiveBroadcastCard(
            "_typename",
            "cluster_id",
            graphqlUser,
            graphqlBroadcast
        )

        val liveCard = graphqlLiveBroadcastCard.toLiveCard(2)
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
    }

    @Test
    fun `the creator card from the v5 API can be turned to an offline card`() {
        val graphqlUser = LobbyQuery.User1(
            "_typename",
            LobbyQuery.User1.Fragments(getUserFragment("caid1", "user1"))
        )
        val graphqlCreatorCard = LobbyQuery.AsCreatorCard(
            "_typename",
            "cluster_id",
            graphqlUser
        )

        val offlineCard = graphqlCreatorCard.toOfflineCard(2)
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
    ) = UserFragment(
        "_typename",
        caid,
        username,
        "/avatar_image_path/$username.jpg",
        isFollowing,
        isCaster,
        isVerified
    )
}