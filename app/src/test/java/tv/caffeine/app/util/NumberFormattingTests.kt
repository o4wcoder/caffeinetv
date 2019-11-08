package tv.caffeine.app.util

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class FrenchNumberFormattingTests {
    @get:Rule val localRule = DefaultLocaleTestRule(Locale.FRANCE)

    @Test
    fun `verify thousands of followers`() {
        val followers = 2323
        assertEquals("2,3\u00a0k", compactThousandsNumberFormat(followers))
        assertEquals("2\u00a0323", longFormThousandsNumberFormat(followers))
    }

    @Test
    fun `verify million two followers`() {
        val followers = 1_200_000
        assertEquals("1,2\u00a0M", compactThousandsNumberFormat(followers))
        assertEquals("1,2\u00a0M", longFormThousandsNumberFormat(followers))
    }

    @Test
    fun `verify two million followers`() {
        val followers = 2_000_000
        assertEquals("2\u00a0M", compactThousandsNumberFormat(followers))
        assertEquals("2\u00a0M", longFormThousandsNumberFormat(followers))
    }

    @Test
    fun `verify billion followers`() {
        val followers = 1_000_000_000
        assertEquals("1\u00a0Md", compactThousandsNumberFormat(followers))
        assertEquals("1\u00a0Md", longFormThousandsNumberFormat(followers))
    }
}

@RunWith(RobolectricTestRunner::class)
class NumberFormattingTests {

    @Test
    fun `verify zero followers`() {
        val followers = 0
        assertEquals("0", compactThousandsNumberFormat(followers))
        assertEquals("0", longFormThousandsNumberFormat(followers))
    }

    @Test
    fun `verify one follower`() {
        val followers = 1
        assertEquals("1", compactThousandsNumberFormat(followers))
        assertEquals("1", longFormThousandsNumberFormat(followers))
    }

    @Test
    fun `verify five hundred followers`() {
        val followers = 500
        assertEquals("500", compactThousandsNumberFormat(followers))
        assertEquals("500", longFormThousandsNumberFormat(followers))
    }

    @Test
    fun `verify thousand followers`() {
        val followers = 1000
        assertEquals("1K", compactThousandsNumberFormat(followers))
        assertEquals("1,000", longFormThousandsNumberFormat(followers))
    }

    @Test
    fun `verify two thousand followers`() {
        val followers = 2000
        assertEquals("2K", compactThousandsNumberFormat(followers))
        assertEquals("2,000", longFormThousandsNumberFormat(followers))
    }

    @Test
    fun `verify eleven thousand followers`() {
        val followers = 11_000
        assertEquals("11K", compactThousandsNumberFormat(followers))
        assertEquals("11K", longFormThousandsNumberFormat(followers))
    }

    @Test
    fun `verify ninety nine thousand nine hundred ninety nine followers`() {
        val followers = 99_999
        assertEquals("99.9K", compactThousandsNumberFormat(followers))
        assertEquals("99.9K", longFormThousandsNumberFormat(followers))
    }

    @Test
    fun `verify hundreds of thousands followers`() {
        val followers = 999_999
        assertEquals("999.9K", compactThousandsNumberFormat(followers))
        assertEquals("999.9K", longFormThousandsNumberFormat(followers))
    }

    @Test
    fun `verify million followers`() {
        val followers = 1_000_000
        assertEquals("1M", compactThousandsNumberFormat(followers))
        assertEquals("1M", longFormThousandsNumberFormat(followers))
    }

    @Test
    fun `verify million two followers`() {
        val followers = 1_200_000
        assertEquals("1.2M", compactThousandsNumberFormat(followers))
        assertEquals("1.2M", longFormThousandsNumberFormat(followers))
    }

    @Test
    fun `verify two million followers`() {
        val followers = 2_000_000
        assertEquals("2M", compactThousandsNumberFormat(followers))
        assertEquals("2M", longFormThousandsNumberFormat(followers))
    }

    @Test
    fun `verify billion followers`() {
        val followers = 1_000_000_000
        assertEquals("1B", compactThousandsNumberFormat(followers))
        assertEquals("1B", longFormThousandsNumberFormat(followers))
    }

    @Test
    fun `verify two billion followers`() {
        val followers = 2_000_000_000
        assertEquals("2B", compactThousandsNumberFormat(followers))
        assertEquals("2B", longFormThousandsNumberFormat(followers))
    }
}

@RunWith(RobolectricTestRunner::class)
class EnglishFallbackNumberFormattingTests {

    @Test
    fun `verify 0 followers`() {
        val followers = 0
        assertEquals("0", englishCompactNumberFormatWithDecimal(followers))
    }

    @Test
    fun `verify one follower`() {
        val followers = 1
        assertEquals("1", englishCompactNumberFormatWithDecimal(followers))
    }

    @Test
    fun `verify five hundred followers`() {
        val followers = 500
        assertEquals("500", englishCompactNumberFormatWithDecimal(followers))
    }

    @Test
    fun `verify thousand followers`() {
        val followers = 1000
        assertEquals("1K", englishCompactNumberFormatWithDecimal(followers))
    }

    @Test
    fun `verify two thousand followers`() {
        val followers = 2000
        assertEquals("2K", englishCompactNumberFormatWithDecimal(followers))
    }

    @Test
    fun `verify eleven thousand followers`() {
        val followers = 11_000
        assertEquals("11K", englishCompactNumberFormatWithDecimal(followers))
    }

    @Test
    fun `verify ninety nine thousand nine hundred ninety nine followers`() {
        val followers = 99_999
        assertEquals("99.9K", englishCompactNumberFormatWithDecimal(followers))
    }

    @Test
    fun `verify hundreds of thousands followers`() {
        val followers = 999_999
        assertEquals("999.9K", englishCompactNumberFormatWithDecimal(followers))
    }

    @Test
    fun `verify million followers`() {
        val followers = 1_000_000
        assertEquals("1M", englishCompactNumberFormatWithDecimal(followers))
    }

    @Test
    fun `verify million two followers`() {
        val followers = 1_200_000
        assertEquals("1.2M", englishCompactNumberFormatWithDecimal(followers))
    }

    @Test
    fun `verify two million followers`() {
        val followers = 2_000_000
        assertEquals("2M", englishCompactNumberFormatWithDecimal(followers))
    }

    @Test
    fun `verify billion followers`() {
        val followers = 1_000_000_000
        assertEquals("1B", englishCompactNumberFormatWithDecimal(followers))
    }

    @Test
    fun `verify two billion followers`() {
        val followers = 2_000_000_000
        assertEquals("2B", englishCompactNumberFormatWithDecimal(followers))
    }
}