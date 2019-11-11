package tv.caffeine.app.users

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.caffeine.app.R
import tv.caffeine.app.api.model.User
import tv.caffeine.app.databinding.CaidItemBinding
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.TestDispatchConfig

class UserListAdapterTests {
    lateinit var subject: UserListAdapter

    @MockK lateinit var followManager: FollowManager
    @MockK lateinit var user: User
    @MockK lateinit var containerView: ViewGroup
    @MockK(relaxed = true) lateinit var binding: CaidItemBinding

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkStatic(LayoutInflater::class)
        every { LayoutInflater.from(any()) } returns mockk()
        mockkStatic(DataBindingUtil::class)
        every { DataBindingUtil.inflate<CaidItemBinding>(any(), R.layout.caid_item, any(), false) } returns binding
        every { containerView.context } returns mockk()
        subject = UserListAdapter(TestDispatchConfig, followManager)
    }

    @Test
    fun `if following is allowed in adapter, it is allowed in view holder`() {
        subject.allowFollowing = true
        val viewHolder = subject.onCreateViewHolder(containerView, 0)
        assertTrue(viewHolder.allowFollowing)
    }

    @Test
    fun `if following is not allowed in adapter, it is not allowed in view holder`() {
        subject.allowFollowing = false
        val viewHolder = subject.onCreateViewHolder(containerView, 0)
        assertFalse(viewHolder.allowFollowing)
    }
}
