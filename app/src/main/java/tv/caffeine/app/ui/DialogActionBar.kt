package tv.caffeine.app.ui

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import tv.caffeine.app.R

class DialogActionBar @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayout(context, attrs, defStyleAttr) {

    private val container: ViewGroup
    private val closeImageView: ImageView
    private val titleTextView: TextView
    private val actionTextView: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.dialog_action_bar, this, true)
        container = findViewById(R.id.container)
        closeImageView = findViewById(R.id.close_image_view)
        titleTextView = findViewById(R.id.title_text_view)
        actionTextView = findViewById(R.id.action_text_view)
    }

    fun setTitle(text: CharSequence) {
        titleTextView.text = text
    }

    fun setTitle(@StringRes resId: Int) {
        titleTextView.setText(resId)
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

    fun applyDarkMode() {
        container.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent))
        closeImageView.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
    }
}