package tv.caffeine.app.ui

import android.text.Spannable
import android.text.style.ImageSpan
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.DimenRes
import androidx.core.content.getSystemService
import androidx.core.text.HtmlCompat
import androidx.databinding.BindingAdapter
import com.squareup.picasso.Picasso
import tv.caffeine.app.R
import tv.caffeine.app.util.getPicasso

inline fun EditText.setOnActionGo(crossinline block: () -> Unit) {
    setOnEditorActionListener { _, actionId, _ ->
        return@setOnEditorActionListener when (actionId) {
            EditorInfo.IME_ACTION_GO -> {
                block()
                context.getSystemService<InputMethodManager>()
                        ?.hideSoftInputFromWindow(windowToken, 0)
                true
            }
            else -> false
        }
    }
}

inline fun EditText.setOnAction(action: Int, crossinline block: () -> Unit) {
    setOnEditorActionListener { _, actionId, _ ->
        return@setOnEditorActionListener when (actionId) {
            action -> {
                block()
                context.getSystemService<InputMethodManager>()
                        ?.hideSoftInputFromWindow(windowToken, 0)
                true
            }
            else -> false
        }
    }
}

fun EditText.prepopulateText(text: String?) {
    setText(text)
    text?.length?.let {
        setSelection(it)
        append(" ")
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
    formatUsernameAsHtml(context.getPicasso(), string, false)
}
