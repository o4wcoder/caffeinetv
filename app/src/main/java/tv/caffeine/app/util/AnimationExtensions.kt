package tv.caffeine.app.util

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import tv.caffeine.app.R

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

fun View.fadeOut() {
    ValueAnimator.ofFloat(1f, 0f).apply {
        duration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        addUpdateListener {
            alpha = animatedValue as Float
        }
        addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator?) {
                alpha = 0f
                isInvisible = true
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

fun View.fadeOutLoadingIndicator() {
    ValueAnimator.ofFloat(1f, 0f).apply {
        duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        startDelay = resources.getInteger(R.integer.loading_indicator_fade_out_start_delay).toLong()
        addUpdateListener {
            (animatedValue as Float).let {
                alpha = it
                scaleX = it
                scaleY = it
            }
        }
        addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator?) {
                isInvisible = true
                alpha = 1f
                scaleX = 1f
                scaleY = 1f
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
