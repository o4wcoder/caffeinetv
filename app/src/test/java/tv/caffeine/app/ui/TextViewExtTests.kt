package tv.caffeine.app.ui

import android.content.Context
import android.text.SpannedString
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TextViewExtTests {

    private lateinit var context: Context
    private lateinit var textView: TextView

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        textView = TextView(context)
    }

    @Test
    fun `The text is cleared if a null string is set as the html text`() {
        textView.text = "a"
        textView.configureHtmlText(null)
        assertEquals("", textView.text)
    }

    @Test
    fun `The text is the parsed html text if a non-null string is set as the html text`() {
        val htmlText = "<b>a</b>"
        textView.configureHtmlText(htmlText)
        assertNotEquals(htmlText, textView.text)
        assertTrue(textView.text is SpannedString)
    }
}