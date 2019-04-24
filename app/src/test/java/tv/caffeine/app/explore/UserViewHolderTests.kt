package tv.caffeine.app.explore

import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.R
import tv.caffeine.app.api.SearchUserItem
import tv.caffeine.app.api.model.User
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.UserTheme
import tv.caffeine.app.util.configure

@RunWith(RobolectricTestRunner::class)
class UserViewHolderTests {

    private val caid = "123"
    private val followingTheme = UserTheme(R.style.ExploreUsername_Following)
    private val notFollowingTheme = UserTheme(R.style.ExploreUsername_NotFollowing)
    private lateinit var userViewHolder: UserViewHolder

    @MockK lateinit var user: User
    @MockK lateinit var followManager: FollowManager
    @MockK lateinit var followHandler: FollowManager.FollowHandler

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        val context = InstrumentationRegistry.getInstrumentation().context
        val itemView = LayoutInflater.from(context).inflate(R.layout.user_item_search, FrameLayout(context))
        userViewHolder = UserViewHolder(itemView, followHandler)

        mockkStatic("tv.caffeine.app.util.UsernameThemingKt")
        every {
            user.configure(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns Unit
        every { followManager.isFollowing(any()) } returns false
        every { followManager.followersLoaded() } returns true
    }

    @Test
    fun `show the follow button if the user is not me`() {
        every { followManager.isSelf(any()) } returns false
        val searchUserItem = SearchUserItem(user, 0f, caid)
        userViewHolder.bind(searchUserItem, followManager, followingTheme, notFollowingTheme)
        assertTrue(userViewHolder.followButton.isVisible)
    }

    @Test
    fun `do not show the follow button if the user is me`() {
        every { followManager.isSelf(any()) } returns true
        val searchUserItem = SearchUserItem(user, 0f, caid)
        userViewHolder.bind(searchUserItem, followManager, followingTheme, notFollowingTheme)
        assertFalse(userViewHolder.followButton.isVisible)
    }
}