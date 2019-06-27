package tv.caffeine.app.util

import androidx.collection.LruCache

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val MAX_SIZE = 1000

class LruCacheExtTests {
    lateinit var subject: LruCache<String, Long>

    @Before
    fun setup() {
        subject = LruCache(MAX_SIZE)
    }

    @Test
    fun `putIfAbsent adds new key to cache`() {
        val key = "1"
        val value = 1L
        assertTrue(subject.putIfAbsent(key, value))
        assertTrue(subject.get(key) == value)
    }

    @Test
    fun `putIfAbsent does not add existing key to cache`() {
        val key = "1"
        val value = 1L
        val valueToIgnore = 2L
        subject.put(key, value)
        assertFalse(subject.putIfAbsent(key, valueToIgnore))
        assertTrue(subject.get(key) == value)
    }
}