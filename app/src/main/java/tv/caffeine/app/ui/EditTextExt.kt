package tv.caffeine.app.ui

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.getSystemService

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

/**
 * https://developer.android.com/guide/topics/ui/dialogs.html
 * Tip: By default, when you set an EditText element to use the "textPassword" input type,
 * the font family is set to monospace, so you should change its font family to "sans-serif"
 * so that both text fields use a matching font style.
 *
 * This does not happen on every device - evidence points to Pixels and HTC devices
 *
 * @param inputType the int value of the inputType attribute passed in XML
 */
fun EditText.updateInputType(inputType: Int) {
    if (inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD) {
        val typeface = this.typeface
        this.inputType = inputType
        this.typeface = typeface
    } else {
        this.inputType = inputType
    }
}