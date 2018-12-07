package tv.caffeine.app.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import tv.caffeine.app.R

class DialogActionBar @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    private val closeImageView: View
    private val titleTextView: TextView
    private val actionTextView: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.dialog_action_bar, this, true)
        closeImageView = findViewById(R.id.close_image_view)
        titleTextView = findViewById(R.id.title_text_view)
        actionTextView = findViewById(R.id.action_text_view)
    }

    fun setTitle(text: CharSequence) {
        titleTextView.text = text
    }

    fun setActionText(@StringRes resId: Int) {
        actionTextView.setText(resId)
    }

    fun setActionListener(listener: (View) -> Unit) {
        actionTextView.setOnClickListener(listener)
    }

    fun setDismissListener(listener: (View) -> Unit) {
        closeImageView.setOnClickListener(listener)
    }
}