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
import tv.caffeine.app.R

inline fun EditText.setOnActionGo(crossinline block: () -> Unit) {
    setOnEditorActionListener { _, actionId, _ ->
        return@setOnEditorActionListener when(actionId) {
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

inline fun EditText.showKeyboard() {
    context.getSystemService<InputMethodManager>()
            ?.showSoftInput(this, 0)
}

inline fun EditText.setOnAction(action: Int, crossinline block: () -> Unit) {
    setOnEditorActionListener { _, actionId, _ ->
        return@setOnEditorActionListener when(actionId) {
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

fun TextView.formatUsernameAsHtml(string: String?, isFollowed: Boolean, @DimenRes avatarSizeDimen: Int) {
    text = string?.let { string ->
        val imageGetter = UserAvatarImageGetter(this, isFollowed, avatarSizeDimen)
        val html = HtmlCompat.fromHtml(string, HtmlCompat.FROM_HTML_MODE_LEGACY, imageGetter, null) as Spannable
        for (span in html.getSpans(0, html.length, ImageSpan::class.java)) {
            val start = html.getSpanStart(span)
            val end = html.getSpanEnd(span)
            html.setSpan(CenterImageSpan(span.drawable), start, end, html.getSpanFlags(span))
        }

        html
    }
}

var TextView.htmlText: String?
    get() = null
    set(value) {
        formatUsernameAsHtml(value, false, R.dimen.chat_avatar_size)
    }
