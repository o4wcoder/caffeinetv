package tv.caffeine.app.ui

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import androidx.annotation.ColorRes
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.R

@RunWith(RobolectricTestRunner::class)
class CaffeineEditTextLayoutViewModelTests {

    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

    private val HINT = "Username"
    private val BOTTOM_HINT = "Bottom hint"
    private val ERROR = "Error"

    lateinit var context: Context
    lateinit var subject: CaffeineEditTextLayoutViewModel

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        subject = CaffeineEditTextLayoutViewModel(context)
    }

    @Test
    fun `bottom view is hidden on creation`() {
        subject.setEditTextHint(HINT)
        assertTrue(subject.getBottomViewVisibility() == View.GONE)
    }

    @Test
    fun `edit text text color is correct`() {
        subject.setEditTextHint(HINT)
        assertTrue(subject.getEditTextTextColor() == R.color.caffeine_edit_text_layout_text)
    }

    @Test
    fun `edit text text color is correct in dark mode`() {
        subject.setDarkMode(true)
        subject.setEditTextHint(HINT)
        assertTrue(subject.getEditTextTextColor() == R.color.caffeine_edit_text_layout_text_dark)
    }

    @Test
    fun `edit text hint is correct`() {
        subject.setEditTextHint(HINT)
        assertTrue(subject.getEditTextHint() == HINT)
    }

    @Test
    fun `edit text hint color is correct`() {
        subject.setEditTextHint(HINT)
        assertTrue(subject.getEditTextHintColor() == R.color.caffeine_edit_text_layout_main_hint)
    }

    @Test
    fun `edit text hint color is correct in dark mode`() {
        subject.setDarkMode(true)
        subject.setEditTextHint(HINT)
        assertTrue(subject.getEditTextHintColor() == R.color.caffeine_edit_text_layout_main_hint)
    }

    @Test
    fun `line color is correct`() {
        subject.setEditTextHint(HINT)
        assertTrue(subject.getLineColor() == getColorStateList(R.color.caffeine_edit_text_layout_line))
    }

    @Test
    fun `line color is correct in dark mode`() {
        subject.setDarkMode(true)
        subject.setEditTextHint(HINT)
        assertTrue(subject.getLineColor() == getColorStateList(R.color.caffeine_edit_text_layout_line_dark))
    }

    @Test
    fun `line color is correct with error`() {
        subject.setEditTextHint(HINT)
        subject.setError(ERROR)
        assertTrue(subject.getLineColor() == getColorStateList(R.color.caffeine_edit_text_layout_error))
    }

    @Test
    fun `line color is correct with error in dark mode`() {
        subject.setDarkMode(true)
        subject.setEditTextHint(HINT)
        subject.setError(ERROR)
        assertTrue(subject.getLineColor() == getColorStateList(R.color.caffeine_edit_text_layout_error))
    }

    @Test
    fun `line color is correct when edit text has focus`() {
        subject.setEditTextHint(HINT)
        subject.setEditTextHasFocus(true)
        assertTrue(subject.getLineColor() == getColorStateList(R.color.caffeine_edit_text_layout_line_focused))
    }

    @Test
    fun `line color is correct when edit text has focus in dark mode`() {
        subject.setDarkMode(true)
        subject.setEditTextHint(HINT)
        subject.setEditTextHasFocus(true)
        assertTrue(subject.getLineColor() == getColorStateList(R.color.caffeine_edit_text_layout_line_focused_dark))
    }

    @Test
    fun `line color is correct when edit text is not empty`() {
        subject.setEditTextHint(HINT)
        subject.setEditTextIsEmpty(false)
        assertTrue(subject.getLineColor() == getColorStateList(R.color.caffeine_edit_text_layout_line_finished))
    }

    @Test
    fun `line color is correct when edit text is not empty in dark mode`() {
        subject.setDarkMode(true)
        subject.setEditTextHint(HINT)
        subject.setEditTextIsEmpty(false)
        assertTrue(subject.getLineColor() == getColorStateList(R.color.caffeine_edit_text_layout_line_finished_dark))
    }

    @Test
    fun `line color is correct when edit text is not empty and has focus`() {
        subject.setEditTextHint(HINT)
        subject.setEditTextHasFocus(true)
        subject.setEditTextIsEmpty(false)
        assertTrue(subject.getLineColor() == getColorStateList(R.color.caffeine_edit_text_layout_line_focused))
    }

    @Test
    fun `line color is correct when edit text is not empty and has focus in dark mode`() {
        subject.setDarkMode(true)
        subject.setEditTextHint(HINT)
        subject.setEditTextHasFocus(true)
        subject.setEditTextIsEmpty(false)
        assertTrue(subject.getLineColor() == getColorStateList(R.color.caffeine_edit_text_layout_line_focused_dark))
    }

    @Test
    fun `bottom view color is correct`() {
        subject.setEditTextHint(HINT)
        assertTrue(subject.getBottomViewTextColor() == R.color.caffeine_edit_text_layout_bottom_hint)
    }

    @Test
    fun `bottom view color is correct in dark mode`() {
        subject.setDarkMode(true)
        subject.setEditTextHint(HINT)
        assertTrue(subject.getBottomViewTextColor() == R.color.caffeine_edit_text_layout_bottom_hint_dark)
    }

    @Test
    fun `bottom view color is correct with error`() {
        subject.setEditTextHint(HINT)
        subject.setError(ERROR)
        assertTrue(subject.getBottomViewTextColor() == R.color.caffeine_edit_text_layout_error)
    }

    @Test
    fun `bottom view color is correct with error in dark mode`() {
        subject.setDarkMode(true)
        subject.setEditTextHint(HINT)
        subject.setError(ERROR)
        assertTrue(subject.getBottomViewTextColor() == R.color.caffeine_edit_text_layout_error)
    }

    @Test
    fun `bottom view text is correct when bottom hint not provided`() {
        subject.setEditTextHint(HINT)
        assertTrue(subject.getBottomViewText() == HINT)
    }

    @Test
    fun `bottom view text is correct when bottom hint is provided`() {
        subject.setEditTextHint(HINT)
        subject.setBottomViewText(BOTTOM_HINT, false)
        assertTrue(subject.getBottomViewText() == BOTTOM_HINT)
    }

    @Test
    fun `bottom view text is correct with error when bottom hint not provided`() {
        subject.setEditTextHint(HINT)
        subject.setError(ERROR)
        assertTrue(subject.getBottomViewText() == ERROR)
    }

    @Test
    fun `bottom view text is correct with error when bottom hint is provided`() {
        subject.setEditTextHint(HINT)
        subject.setBottomViewText(BOTTOM_HINT, false)
        subject.setError(ERROR)
        assertTrue(subject.getBottomViewText() == ERROR)
    }

    private fun getColorResource(@ColorRes color: Int): Int {
        return context.resources.getColor(color, null)
    }

    private fun getColorStateList(@ColorRes color: Int): ColorStateList {
        return ColorStateList.valueOf(getColorResource(color))
    }
}