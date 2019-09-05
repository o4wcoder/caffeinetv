package tv.caffeine.app.ui

import android.view.View
import io.mockk.MockKAnnotations
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertEquals

@RunWith(RobolectricTestRunner::class)
class FollowStarViewModelTests {
    private lateinit var subject: FollowStarViewModel
    private val caid = "123"
    private val isFollowing = false

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        subject = FollowStarViewModel(mockk(), mockk(), mockk())
    }

    @Test
    fun `follow star is shown if not self`() {
        val isSelf = false
        subject.bind(caid, isFollowing, isSelf)
        assertEquals(subject.getStarVisibility(), View.VISIBLE)
    }

    @Test
    fun `follow star is invisible if self`() {
        val isSelf = true
        subject.bind(caid, isFollowing, isSelf)
        assertEquals(subject.getStarVisibility(), View.INVISIBLE)
    }

    @Test
    fun `follow star is invisible if hidden`() {
        subject.hide()
        assertEquals(subject.getStarVisibility(), View.INVISIBLE)
    }
}