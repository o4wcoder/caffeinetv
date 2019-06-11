package tv.caffeine.app.util

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible

private const val DEFAULT_PULSE_DURATION = 1250L

class PulseAnimator(val view: View) {

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = DEFAULT_PULSE_DURATION
        addUpdateListener {
            view.alpha = animatedValue as Float
            view.requestLayout()
        }
        doOnEnd {
            startDelay = DEFAULT_PULSE_DURATION
            start()
        }
        interpolator = LinearInterpolator()
        repeatCount = 1
        repeatMode = ValueAnimator.REVERSE
    }

    fun startPulse() {
        if (!animator.isRunning) {
            animator.start()
        }
        view.isVisible = true
    }

    fun stopPulse() {
        animator.end()
        view.isVisible = false
    }
}
