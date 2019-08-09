package tv.caffeine.app.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NumberFormattingTests {

    @Test
    fun `verify zero followers`() {
        val followers = 0
        assertEquals(compactNumberFormat(followers), "0")
    }

    @Test
    fun `verify one follower`() {
        val followers = 1
        assertEquals(compactNumberFormat(followers), "1")
    }

    @Test
    fun `verify five hundred followers`() {
        val followers = 500
        assertEquals(compactNumberFormat(followers), "500")
    }

    @Test
    fun `verify thousand followers`() {
        val followers = 1000
        assertEquals(compactNumberFormat(followers), "1k")
    }

    @Test
    fun `verify two thousand followers`() {
        val followers = 2000
        assertEquals(compactNumberFormat(followers), "2k")
    }

    @Test
    fun `verify million followers`() {
        val followers = 1_000_000
        assertEquals(compactNumberFormat(followers), "1M")
    }

    @Test
    fun `verify two million followers`() {
        val followers = 2_000_000
        assertEquals(compactNumberFormat(followers), "2M")
    }

    @Test
    fun `verify billion followers`() {
        val followers = 1_000_000_000
        assertEquals(compactNumberFormat(followers), "1B")
    }

    @Test
    fun `verify two billion followers`() {
        val followers = 2_000_000_000
        assertEquals(compactNumberFormat(followers), "2B")
    }
}