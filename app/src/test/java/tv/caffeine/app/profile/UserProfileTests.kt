package tv.caffeine.app.profile

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import tv.caffeine.app.R
import tv.caffeine.app.api.model.Broadcast
import tv.caffeine.app.api.model.User
import tv.caffeine.app.session.FollowManager

class UserProfileTests {
    lateinit var subject: UserProfile
    @MockK(relaxed = true) lateinit var user: User
    @MockK(relaxed = true) lateinit var broadcastDetails: Broadcast
    @MockK(relaxed = true) lateinit var followManager: FollowManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `icon is verified for verified caster user`() {
        every { user.isVerified } returns true
        every { user.isCaster } returns true
        subject = UserProfile(user, broadcastDetails, followManager)
        assertEquals(R.drawable.verified, subject.userIcon)
    }

    @Test
    fun `icon is caster for non-verified caster user`() {
        every { user.isVerified } returns false
        every { user.isCaster } returns true
        subject = UserProfile(user, broadcastDetails, followManager)
        assertEquals(R.drawable.caster, subject.userIcon)
    }

    @Test
    fun `icon is verified for verified non-caster user`() {
        every { user.isVerified } returns true
        every { user.isCaster } returns false
        subject = UserProfile(user, broadcastDetails, followManager)
        assertEquals(R.drawable.verified, subject.userIcon)
    }

    @Test
    fun `icon is none for non-verified non-caster user`() {
        every { user.isVerified } returns false
        every { user.isCaster } returns false
        subject = UserProfile(user, broadcastDetails, followManager)
        assertEquals(0, subject.userIcon)
    }

    @Test
    fun `broadcast image is not null when user is live`() {
        val someLink = "https://some-link"
        every { broadcastDetails.mainPreviewImageUrl } returns someLink
        every { broadcastDetails.state } returns Broadcast.State.ONLINE
        subject = UserProfile(user, broadcastDetails, followManager)
        assertNotNull(subject.stageImageUrl)
        assertEquals(someLink, subject.stageImageUrl)
    }

    @Test
    fun `broadcast image is null when user is offline`() {
        val someLink = "https://some-link"
        every { broadcastDetails.mainPreviewImageUrl } returns someLink
        every { broadcastDetails.state } returns Broadcast.State.OFFLINE
        subject = UserProfile(user, broadcastDetails, followManager)
        assertNull(subject.stageImageUrl)
    }

    @Test
    fun `following follower counts correct with tens number`() {
        numberFormatTest(42, "42", "42")
    }

    @Test
    fun `following follower counts correct with hundred thousands number`() {
        numberFormatTest(989_999, "989.9K", "989.9K")
    }

    @Test
    fun `following follower counts correct with thousands number`() {
        numberFormatTest(9899, "9.8K", "9,899")
    }

    private fun numberFormatTest(number: Int, compactString: String, longFormString: String) {
        every { user.followersCount } returns number
        every { user.followingCount } returns number
        subject = UserProfile(user, broadcastDetails, followManager)
        assertEquals(compactString, subject.getFollowersString())
        assertEquals(compactString, subject.getFollowingString())
        assertEquals(longFormString, subject.getMyFollowersString())
        assertEquals(longFormString, subject.getMyFollowingString())
    }
}
