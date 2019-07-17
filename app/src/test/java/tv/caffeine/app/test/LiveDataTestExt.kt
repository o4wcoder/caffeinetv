package tv.caffeine.app.test

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import org.junit.Assert.fail

fun <T> LiveData<T>.observeForTesting(block: (T) -> Unit) {
    val observer = Observer<T> { Unit }
    try {
        observeForever(observer)
        val data = value ?: return fail("Expected LiveData value")
        block(data)
    } finally {
        removeObserver(observer)
    }
}
