package tv.caffeine.app.util

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.core.view.isVisible

private const val DEFAULT_BLINK_ANIMATION_DURATION = 1000L

fun View.animateSlideUpAndHide() {
    ValueAnimator.ofInt(height, 0).apply {
        duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        addUpdateListener {
            layoutParams.height = animatedValue as Int
            requestLayout()
        }
        addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator?) {
                isVisible = false
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }

            override fun onAnimationCancel(animation: Animator?) {
                onAnimationEnd(animation)
            }

            override fun onAnimationStart(animation: Animator?) {
            }

            override fun onAnimationRepeat(animation: Animator?) {
            }
        })
        start()
    }
}

fun View.blink() {
    ValueAnimator.ofFloat(0f, 1f).apply {
        duration = DEFAULT_BLINK_ANIMATION_DURATION
        addUpdateListener {
            alpha = animatedValue as Float
            requestLayout()
        }
        interpolator = LinearInterpolator()
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        start()
    }
}