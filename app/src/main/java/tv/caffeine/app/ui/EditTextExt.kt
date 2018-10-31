package tv.caffeine.app.ui

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.getSystemService
import androidx.core.text.HtmlCompat

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

var TextView.htmlText: String?
    get() = null
    set(value) {
        text = value?.let { string ->
            val imageGetter = UserAvatarImageGetter(this)
            HtmlCompat.fromHtml(string, HtmlCompat.FROM_HTML_MODE_LEGACY, imageGetter, null)
        }
    }
