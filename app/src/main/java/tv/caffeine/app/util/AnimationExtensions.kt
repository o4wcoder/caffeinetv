package tv.caffeine.app.util

import android.animation.ValueAnimator
import android.view.View
import androidx.core.view.isVisible

fun View.animateSlideUpAndHide(originalHeight: Int = height) {
    ValueAnimator.ofInt(originalHeight, 0).apply {
        duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        addUpdateListener {
            when (val newHeight = animatedValue as Int) {
                0 -> {
                    isVisible = false
                    layoutParams.height = originalHeight
                }
                else -> layoutParams.height = newHeight
            }
            requestLayout()
        }
        start()
    }
}
