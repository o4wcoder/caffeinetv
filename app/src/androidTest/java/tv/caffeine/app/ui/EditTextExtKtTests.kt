package tv.caffeine.app.ui

import android.text.InputType
import android.view.ContextThemeWrapper
import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import tv.caffeine.app.CaffeineApplication
import tv.caffeine.app.R

@RunWith(AndroidJUnit4::class)
class EditTextExtKtTests {

    private val textPasswordValue = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    val context: CaffeineApplication = ApplicationProvider.getApplicationContext()

    private val editText = EditText(ContextThemeWrapper(context, R.style.CaffeineEditTextLayoutHint), null, 0)

    @Test
    fun testNotUsingUpdateInputTypeChangesTypeface() {
        val typeface = editText.typeface
        editText.inputType = textPasswordValue
        val newTypeface = editText.typeface
        assertFalse(typeface == newTypeface)
    }

    @Test
    fun testUsingUpdateInputTypeDoesNotChangeTypeface() {
        val typeface = editText.typeface
        editText.updateInputType(textPasswordValue)
        val newTypeface = editText.typeface
        assertTrue(typeface == newTypeface)
    }
}