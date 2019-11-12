package tv.caffeine.app.users

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.R

@RunWith(RobolectricTestRunner::class)
class FollowListViewModelTests {

    lateinit var subject: FollowListViewModel
    lateinit var context: Context

    @MockK lateinit var pagedFollowersService: PagedFollowersService
    val isEmptyState = MutableLiveData<Boolean>()
    val isRefreshingState = MutableLiveData<Boolean>()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { pagedFollowersService.isEmptyState } returns isEmptyState
        every { pagedFollowersService.isRefreshingState } returns isRefreshingState
        context = InstrumentationRegistry.getInstrumentation().context
        subject = FollowersViewModel(context, pagedFollowersService)
    }

    @Test
    fun `when empty follow list show empty message text`() {
        isEmptyState.value = true
        subject.getEmptyMessageVisibility().observeForever { visibility ->
            assertEquals(View.VISIBLE, visibility)
        }
    }

    @Test
    fun `when non-empty follow list do not show empty message text`() {
        isEmptyState.value = false
        subject.getEmptyMessageVisibility().observeForever { visibility ->
            assertEquals(View.GONE, visibility)
        }
    }

    @Test
    fun `dark mode uses white text color for empty message`() {
        subject.isDarkMode = true
        assertEquals(subject.getEmptyMessageTextColor(), ContextCompat.getColor(context, R.color.white))
    }

    @Test
    fun `light mode uses black text color for empty message`() {
        subject.isDarkMode = false
        assertEquals(subject.getEmptyMessageTextColor(), ContextCompat.getColor(context, R.color.black))
    }
}
