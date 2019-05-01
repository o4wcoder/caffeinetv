package tv.caffeine.app.stage

import android.content.Intent
import android.content.res.Resources
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.threeten.bp.Clock
import tv.caffeine.app.profile.UserProfile
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class StageShareIntentBuilderTests {

    private val username = "username"
    private val broadcastName = "broadcastName"
    private val twitterUsername = "@twitterUsername"
    private val timestamp = 123000L
    private val sharerId = "CAID456"
    private lateinit var resources: Resources

    @MockK lateinit var clock: Clock
    @MockK lateinit var userProfile: UserProfile

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        resources = InstrumentationRegistry.getInstrumentation().context.resources
        every { clock.millis() } returns timestamp
        every { userProfile.username } returns username
        every { userProfile.broadcastName } returns broadcastName
        every { userProfile.twitterUsername } returns twitterUsername
        every { userProfile.isMe } returns false
    }

    @Test
    fun `share text matches the pattern when other's stage is shared and the twitter username is available`() {
        val shareText = getShareText(StageShareIntentBuilder(userProfile, sharerId, resources, clock).build())
        assertEquals("Watching username LIVE: \"broadcastName\". #caffeinetv @twitterUsername https://www.caffeine.tv/username?bst=true&share_time=123&sharer_id=CAID456", shareText)
    }

    @Test
    fun `share text matches the pattern when other's stage is shared and the twitter username is unavailable`() {
        every { userProfile.twitterUsername } returns null
        val shareText = getShareText(StageShareIntentBuilder(userProfile, sharerId, resources, clock).build())
        assertEquals("Watching username LIVE: \"broadcastName\". #caffeinetv https://www.caffeine.tv/username?bst=true&share_time=123&sharer_id=CAID456", shareText)
    }

    @Test
    fun `share text matches the pattern when the user's own stage is shared`() {
        every { userProfile.isMe } returns true
        val shareText = getShareText(StageShareIntentBuilder(userProfile, sharerId, resources, clock).build())
        assertEquals("I'm LIVE: \"broadcastName\". #caffeinetv https://www.caffeine.tv/username?bst=true&share_time=123&sharer_id=CAID456", shareText)
    }

    @Test
    fun `share text includes the caffeine username and broadcast name`() {
        val shareText = getShareText(StageShareIntentBuilder(userProfile, sharerId, resources, clock).build())
        assertTrue(username in shareText)
        assertTrue(broadcastName in shareText)
    }

    @Test
    fun `share text includes the twitter username if available`() {
        val shareText = getShareText(StageShareIntentBuilder(userProfile, sharerId, resources, clock).build())
        assertTrue(twitterUsername in shareText)
    }

    @Test
    fun `share text does not include the twitter username if unavailable`() {
        every { userProfile.twitterUsername } returns null
        val shareText = getShareText(StageShareIntentBuilder(userProfile, sharerId, resources, clock).build())
        assertFalse(twitterUsername in shareText)
    }

    @Test
    fun `own stage's share text includes the broadcast name, the username in the url, but not the twitter username`() {
        every { userProfile.isMe } returns true
        val shareText = getShareText(StageShareIntentBuilder(userProfile, sharerId, resources, clock).build())
        assertTrue("/$username" in shareText)
        assertTrue(broadcastName in shareText)
        assertFalse(twitterUsername in shareText)
    }

    @Test
    fun `share with twitter card`() {
        val shareText = getShareText(StageShareIntentBuilder(userProfile, sharerId, resources, clock).build())
        assertTrue("bst=true" in shareText)
    }

    @Test
    fun `share text includes the timestamp`() {
        val shareText = getShareText(StageShareIntentBuilder(userProfile, sharerId, resources, clock).build())
        assertTrue("share_time=${TimeUnit.MILLISECONDS.toSeconds(timestamp)}" in shareText)
    }

    @Test
    fun `share text includes the sharer id if available`() {
        val shareText = getShareText(StageShareIntentBuilder(userProfile, sharerId, resources, clock).build())
        assertTrue("sharer_id=$sharerId" in shareText)
    }

    @Test
    fun `share text does not include the sharer id if unavailable`() {
        val shareText = getShareText(StageShareIntentBuilder(userProfile, null, resources, clock).build())
        assertFalse("sharer_id=" in shareText)
    }

    private fun getShareText(intent: Intent): String {
        return intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT).getStringExtra(Intent.EXTRA_TEXT)
    }
}

