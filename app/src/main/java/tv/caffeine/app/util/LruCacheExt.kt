package tv.caffeine.app.util

import androidx.collection.LruCache

fun LruCache<String, Long>.putIfAbsent(key: String, value: Long): Boolean {
    return if (this.get(key) == null) {
        this.put(key, value)
        true
    } else {
        false
    }
}