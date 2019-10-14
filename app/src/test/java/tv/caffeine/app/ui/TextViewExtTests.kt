package tv.caffeine.app.ui

import android.text.SpannedString
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.CaffeineApplication
import tv.caffeine.app.di.DaggerTestComponent
import tv.caffeine.app.di.setApplicationInjector

@RunWith(RobolectricTestRunner::class)
class TextViewExtTests {

    private lateinit var textView: TextView

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<CaffeineApplication>()
        val testComponent = DaggerTestComponent.factory().create(app)
        app.setApplicationInjector(testComponent)
        val context = InstrumentationRegistry.getInstrumentation().context
        textView = TextView(context)
    }

    @Test
    fun `The text is cleared if a null string is set as the html text`() {
        textView.text = "a"
        textView.formatHtmlText(null)
        assertEquals("", textView.text)
    }

    @Test
    fun `The text is the parsed html text if a non-null string is set as the html text`() {
        val htmlText = "<b>a</b>"
        textView.formatHtmlText(htmlText)
        assertNotEquals(htmlText, textView.text)
        assertTrue(textView.text is SpannedString)
    }
}