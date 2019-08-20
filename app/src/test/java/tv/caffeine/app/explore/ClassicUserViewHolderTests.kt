package tv.caffeine.app.explore

import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.R
import tv.caffeine.app.api.SearchUserItem
import tv.caffeine.app.api.model.User
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.configure

@RunWith(RobolectricTestRunner::class)
class ClassicUserViewHolderTests {

    private val caid = "123"
    private lateinit var userViewHolder: ClassicUserViewHolder

    @MockK(relaxed = true) lateinit var user: User
    @MockK lateinit var followManager: FollowManager
    @MockK lateinit var followHandler: FollowManager.FollowHandler

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        val context = InstrumentationRegistry.getInstrumentation().context
        val itemView = LayoutInflater.from(context).inflate(R.layout.user_item_search, FrameLayout(context))
        userViewHolder = ClassicUserViewHolder(itemView, followHandler)

        mockkStatic("tv.caffeine.app.util.UsernameThemingKt")
        every {
            user.configure(any(), any(), any(), any(), any(), any(), any(), any())
        } returns Unit
        every { followManager.isFollowing(any()) } returns false
        every { followManager.followersLoaded() } returns true
    }

    @Test
    fun `show the follow button if the user is not me`() {
        every { followManager.isSelf(any()) } returns false
        val searchUserItem = SearchUserItem(user, 0f, caid)
        userViewHolder.bind(searchUserItem, followManager)
        assertTrue(userViewHolder.followButton.isVisible)
    }

    @Test
    fun `do not show the follow button if the user is me`() {
        every { followManager.isSelf(any()) } returns true
        val searchUserItem = SearchUserItem(user, 0f, caid)
        userViewHolder.bind(searchUserItem, followManager)
        assertFalse(userViewHolder.followButton.isVisible)
    }
}
