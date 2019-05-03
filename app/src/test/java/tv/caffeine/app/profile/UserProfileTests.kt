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
import java.text.NumberFormat

class UserProfileTests {
    lateinit var subject: UserProfile
    @MockK(relaxed = true) lateinit var user: User
    @MockK(relaxed = true) lateinit var broadcastDetails: Broadcast
    @MockK(relaxed = true) lateinit var numberFormat: NumberFormat
    @MockK(relaxed = true) lateinit var followManager: FollowManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `icon is verified for verified caster user`() {
        every { user.isVerified } returns true
        every { user.isCaster } returns true
        subject = UserProfile(user, broadcastDetails, numberFormat, followManager)
        assertEquals(R.drawable.verified, subject.userIcon)
    }

    @Test
    fun `icon is caster for non-verified caster user`() {
        every { user.isVerified } returns false
        every { user.isCaster } returns true
        subject = UserProfile(user, broadcastDetails, numberFormat, followManager)
        assertEquals(R.drawable.caster, subject.userIcon)
    }

    @Test
    fun `icon is verified for verified non-caster user`() {
        every { user.isVerified } returns true
        every { user.isCaster } returns false
        subject = UserProfile(user, broadcastDetails, numberFormat, followManager)
        assertEquals(R.drawable.verified, subject.userIcon)
    }

    @Test
    fun `icon is none for non-verified non-caster user`() {
        every { user.isVerified } returns false
        every { user.isCaster } returns false
        subject = UserProfile(user, broadcastDetails, numberFormat, followManager)
        assertEquals(0, subject.userIcon)
    }

    @Test
    fun `broadcast image is not null when user is live`() {
        val someLink = "https://some-link"
        every { broadcastDetails.mainPreviewImageUrl } returns someLink
        every { broadcastDetails.state } returns Broadcast.State.ONLINE
        subject = UserProfile(user, broadcastDetails, numberFormat, followManager)
        assertNotNull(subject.stageImageUrl)
        assertEquals(someLink, subject.stageImageUrl)
    }

    @Test
    fun `broadcast image is null when user is offline`() {
        val someLink = "https://some-link"
        every { broadcastDetails.mainPreviewImageUrl } returns someLink
        every { broadcastDetails.state } returns Broadcast.State.OFFLINE
        subject = UserProfile(user, broadcastDetails, numberFormat, followManager)
        assertNull(subject.stageImageUrl)
    }

}
