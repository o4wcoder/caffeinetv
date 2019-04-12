package tv.caffeine.app.ui

import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.databinding.BindingAdapter
import tv.caffeine.app.R

@BindingAdapter("userIcon")
fun TextView.configureUserIcon(@DrawableRes userIcon: Int) {
    setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, userIcon, 0)
    compoundDrawablePadding = if (userIcon != 0) resources.getDimensionPixelSize(R.dimen.margin_line_spacing_small) else 0
}
