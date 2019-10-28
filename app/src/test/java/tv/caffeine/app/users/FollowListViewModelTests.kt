package tv.caffeine.app.users

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.R
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.repository.ProfileRepository
import tv.caffeine.app.session.FollowManager

@RunWith(RobolectricTestRunner::class)
class FollowListViewModelTests {

    lateinit var subject: FollowListViewModel
    lateinit var context: Context

    @MockK lateinit var followManager: FollowManager
    @MockK lateinit var profileRepository: ProfileRepository
    @MockK lateinit var usersService: UsersService
    @MockK lateinit var gson: Gson

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        context = InstrumentationRegistry.getInstrumentation().context
        subject = FollowersViewModel(context, gson, usersService, followManager, profileRepository)
    }

    @Test
    fun `when empty follow list show empty message text`() {
        val emptyCaidList = listOf<CaidRecord>()
        subject.isEmptyFollowList = emptyCaidList.isEmpty()
        assertEquals(subject.getEmptyMessageVisibility(), View.VISIBLE)
    }

    @Test
    fun `when non-empty follow list do not show empty message text`() {
        var caidList = mutableListOf<CaidRecord>()
        val mockCaidRecord = mockk<CaidRecord>()
        caidList.add(mockCaidRecord)
        subject.isEmptyFollowList = caidList.isEmpty()
        assertEquals(subject.getEmptyMessageVisibility(), View.GONE)
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
