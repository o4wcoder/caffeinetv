package tv.caffeine.app.ui

import android.widget.TextView
import androidx.core.content.ContextCompat
import tv.caffeine.app.R

class FollowButtonDecorator(val style: Style) {
    fun decorate(button: TextView) {
        button.setText(when (style) {
            Style.FOLLOW -> R.string.follow_button
            Style.FOLLOWING -> R.string.following_button
        })
        button.setTextColor(ContextCompat.getColor(button.context, when (style) {
            Style.FOLLOW -> R.color.caffeine_blue
            Style.FOLLOWING -> R.color.white
        }))
        button.backgroundTintList = when (style) {
            Style.FOLLOW -> ContextCompat.getColorStateList(button.context, R.color.transparent)
            Style.FOLLOWING -> ContextCompat.getColorStateList(button.context, R.color.caffeine_blue)
        }
    }

    enum class Style {
        FOLLOW, FOLLOWING
    }
}