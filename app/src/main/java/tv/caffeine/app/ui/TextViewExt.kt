package tv.caffeine.app.ui

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ImageSpan
import android.text.style.URLSpan
import android.widget.TextView
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.core.text.HtmlCompat
import androidx.databinding.BindingAdapter
import com.squareup.picasso.Picasso
import tv.caffeine.app.R
import tv.caffeine.app.util.convertLinks
import tv.caffeine.app.util.getPicasso

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

fun TextView.stripUrlUnderline() {
    val spannable: Spannable = SpannableString(text)
    val spans = spannable.getSpans(0, spannable.length, URLSpan::class.java)
    for (span in spans) {
        val start = spannable.getSpanStart(span)
        val end = spannable.getSpanEnd(span)
        spannable.removeSpan(span)
        val spanNoUnderline = URLSpanNoUnderline(span.url)
        spannable.setSpan(spanNoUnderline, start, end, 0)
    }
    text = spannable
}

@BindingAdapter("android:textAppearance")
fun TextView.configureTextAppearance(@StyleRes textAppearanceRes: Int) = setTextAppearance(textAppearanceRes)

open class URLSpanNoUnderline(url: String) : URLSpan(url) {
    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.isUnderlineText = false
    }
}

fun TextView.formatUsernameAsHtml(
    picasso: Picasso,
    string: String?,
    isFollowed: Boolean = false,
    @DimenRes avatarSizeDimen: Int = R.dimen.chat_avatar_size
) {
    if (string == null) {
        text = null
        return
    }
    val imageGetter = UserAvatarImageGetter(this, isFollowed, avatarSizeDimen, picasso)
    val html = HtmlCompat.fromHtml(string, HtmlCompat.FROM_HTML_MODE_LEGACY, imageGetter, null) as Spannable
    for (span in html.getSpans(0, html.length, ImageSpan::class.java)) {
        val start = html.getSpanStart(span)
        val end = html.getSpanEnd(span)
        html.setSpan(CenterImageSpan(span.drawable), start, end, html.getSpanFlags(span))
    }
    text = html
}

@BindingAdapter("htmlText")
fun TextView.formatHtmlText(string: String?) {
    formatUsernameAsHtml(context.getPicasso(), string, false, R.dimen.avatar_friends_watching)
}
