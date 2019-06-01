package tv.caffeine.app.ui

import android.view.View
import android.view.ViewGroup
import androidx.databinding.BindingAdapter

fun View.isLayoutLtr() = layoutDirection == View.LAYOUT_DIRECTION_LTR

@BindingAdapter("android:layout_width")
fun View.setLayoutWidth(width: Float) {
    layoutParams.width = width.toInt()
}

@BindingAdapter("android:layout_height")
fun View.setLayoutHeight(height: Float) {
    layoutParams.height = height.toInt()
}

@BindingAdapter("android:layout_marginStart")
fun View.setLayoutMarginStart(margin: Float) {
    (layoutParams as ViewGroup.MarginLayoutParams).apply {
        margin.toInt().let {
            marginStart = it
            if (isLayoutLtr()) leftMargin = it else rightMargin = it
        }
    }
}

@BindingAdapter("android:layout_marginEnd")
fun View.setLayoutMarginEnd(margin: Float) {
    (layoutParams as ViewGroup.MarginLayoutParams).apply {
        margin.toInt().let {
            marginEnd = it
            if (isLayoutLtr()) rightMargin = it else leftMargin = it
        }
    }
}

@BindingAdapter("android:layout_marginTop")
fun View.setLayoutMarginTop(margin: Float) {
    (layoutParams as ViewGroup.MarginLayoutParams).topMargin = margin.toInt()
}

@BindingAdapter("android:layout_marginBottom")
fun View.setLayoutMarginBottom(margin: Float) {
    (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = margin.toInt()
}
