package tv.caffeine.app.ui

import android.view.View
import androidx.databinding.BindingAdapter

@BindingAdapter("android:layout_width")
fun View.setLayoutWidth(width: Float) {
    layoutParams.width = width.toInt()
}

@BindingAdapter("android:layout_height")
fun View.setLayoutHeight(height: Float) {
    layoutParams.height = height.toInt()
}
