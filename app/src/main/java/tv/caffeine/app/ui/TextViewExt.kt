package tv.caffeine.app.ui

import android.graphics.Color
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.core.text.HtmlCompat
import androidx.databinding.BindingAdapter
import tv.caffeine.app.R
import tv.caffeine.app.util.convertLinks

@BindingAdapter("userIcon")
fun TextView.configureUserIcon(@DrawableRes userIcon: Int) {
    setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, userIcon, 0)
    compoundDrawablePadding = if (userIcon != 0) resources.getDimensionPixelSize(R.dimen.verified_icon_padding) else 0
}

fun TextView.configureEmbeddedLink(
    @StringRes stringResId: Int,
    spanClickCallback: (url: String?) -> URLSpan,
    vararg args: Any = emptyArray()
) {
    text = convertLinks(stringResId, resources, spanClickCallback, *args)
    movementMethod = LinkMovementMethod.getInstance()
    highlightColor = Color.TRANSPARENT
}

@BindingAdapter("android:textAppearance")
fun TextView.configureTextAppearance(@StyleRes textAppearanceRes: Int) = setTextAppearance(textAppearanceRes)

@BindingAdapter("htmlText")
fun TextView.configureHtmlText(text: String?) {
    if (text != null) {
        setText(HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY))
    } else {
        setText(null)
    }
}
