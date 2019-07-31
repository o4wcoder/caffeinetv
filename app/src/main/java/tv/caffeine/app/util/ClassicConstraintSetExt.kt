package tv.caffeine.app.util

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import timber.log.Timber
import tv.caffeine.app.R

fun ConstraintLayout.transformToClassicUI() {
    val constraintSet = ConstraintSet()
    constraintSet.clone(this)

    when (this.id) {
        R.id.avatar_username_container -> {
            constraintSet.setHorizontalBias(this.id, 0f)
            constraintSet.setVisibility(R.id.follow_button_image, ConstraintSet.GONE)
            constraintSet.clear(R.id.follow_button_image)

            constraintSet.setVisibility(R.id.more_button, ConstraintSet.VISIBLE)

            constraintSet.clear(R.id.avatar_image_view, ConstraintSet.END)
            constraintSet.connect(R.id.avatar_image_view, ConstraintSet.START, this.id, ConstraintSet.START)
            constraintSet.centerVertically(R.id.avatar_image_view, this.id)

            constraintSet.connect(R.id.broadcast_title_text_view, ConstraintSet.TOP, R.id.username_text_view, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.broadcast_title_text_view, ConstraintSet.START, R.id.avatar_image_view, ConstraintSet.END)
            constraintSet.connect(R.id.broadcast_title_text_view, ConstraintSet.END, R.id.more_button, ConstraintSet.START, 8)
            constraintSet.setMargin(R.id.broadcast_title_text_view, ConstraintSet.BOTTOM, 8)

            constraintSet.connect(R.id.username_text_view, ConstraintSet.START, R.id.avatar_image_view, ConstraintSet.END)
            constraintSet.connect(R.id.username_text_view, ConstraintSet.TOP, this.id, ConstraintSet.TOP)
        }
        R.id.chat_button_layout -> {
            constraintSet.createHorizontalChain(this.id, ConstraintSet.LEFT, this.id, ConstraintSet.RIGHT,
                intArrayOf(R.id.share_button, R.id.chat_button, R.id.gift_button, R.id.friends_watching_button),
                null, ConstraintSet.CHAIN_SPREAD_INSIDE)
            constraintSet.connect(R.id.share_button, ConstraintSet.BOTTOM, this.id, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.share_button, ConstraintSet.END, R.id.chat_button, ConstraintSet.START)
            constraintSet.connect(R.id.share_button, ConstraintSet.START, this.id, ConstraintSet.START)
            constraintSet.connect(R.id.share_button, ConstraintSet.TOP, this.id, ConstraintSet.TOP)
            constraintSet.setMargin(R.id.share_button, ConstraintSet.START, 0)

            constraintSet.connect(R.id.chat_button, ConstraintSet.END, R.id.gift_button, ConstraintSet.START)
            constraintSet.connect(R.id.chat_button, ConstraintSet.START, R.id.share_button, ConstraintSet.END)
            constraintSet.setVisibility(R.id.chat_button, ConstraintSet.VISIBLE)

            constraintSet.connect(R.id.gift_button, ConstraintSet.END, R.id.friends_watching_button, ConstraintSet.START)
            constraintSet.connect(R.id.gift_button, ConstraintSet.START, R.id.chat_button, ConstraintSet.END)
            constraintSet.setMargin(R.id.gift_button, ConstraintSet.END, 0)
            constraintSet.setVisibility(R.id.friends_watching_button, ConstraintSet.VISIBLE)

            constraintSet.setVisibility(R.id.react_button, ConstraintSet.GONE)
        }
        else -> {
            Timber.e("No matching constraint layout id for classic transform")
        }
    }
    constraintSet.applyTo(this)
}