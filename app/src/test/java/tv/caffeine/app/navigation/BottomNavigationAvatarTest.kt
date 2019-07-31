package tv.caffeine.app.navigation

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.MenuItem
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.R
import tv.caffeine.app.ui.BottomNavigationAvatar

@RunWith(RobolectricTestRunner::class)
class BottomNavigationAvatarTest {

    @MockK(relaxed = true) lateinit var menuItem: MenuItem
    lateinit var context: Context
    lateinit var subject: BottomNavigationAvatar
    private var selectedAvatarDrawable: Drawable? = null
    private var unselectedAvatarDrawable: Drawable? = null

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        context = InstrumentationRegistry.getInstrumentation().context
        subject = BottomNavigationAvatar(context, menuItem)
        selectedAvatarDrawable = context.getDrawable(R.drawable.bottom_nav_profile_on)
        unselectedAvatarDrawable = context.getDrawable(R.drawable.bottom_nav_profile_off)
    }

    @Test
    fun `the menu item's icon is the selected avatar if it is selected and the selected-style drawable is available`() {
        subject.selectedAvatarDrawable = selectedAvatarDrawable
        every { menuItem.isChecked } returns true
        subject.updateSelectedState()
        verify(exactly = 1) { menuItem.icon = selectedAvatarDrawable }
    }

    @Test
    fun `the menu item's icon is not overwritten if it is selected and the selected-style drawable is not available`() {
        subject.selectedAvatarDrawable = null
        every { menuItem.isChecked } returns true
        subject.updateSelectedState()
        verify(exactly = 0) { menuItem.icon = any() }
    }

    @Test
    fun `the menu item's icon is the unselected avatar if it is not selected and the unselected-style drawable is not available`() {
        subject.unselectedAvatarDrawable = unselectedAvatarDrawable
        every { menuItem.isChecked } returns false
        subject.updateSelectedState()
        verify(exactly = 1) { menuItem.icon = unselectedAvatarDrawable }
    }

    @Test
    fun `the menu item's icon is not overwritten if it is unselected and the unselected-style drawable is not available`() {
        subject.unselectedAvatarDrawable = null
        every { menuItem.isChecked } returns false
        subject.updateSelectedState()
        verify(exactly = 0) { menuItem.icon = any() }
    }
}