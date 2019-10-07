package tv.caffeine.app.ui

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import tv.caffeine.app.BR
import tv.caffeine.app.R
import tv.caffeine.app.databinding.CaffeineEditTextLayoutBinding
import tv.caffeine.app.util.addFilter

class CaffeineEditTextLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(
    context,
    attrs,
    defStyleAttr) {

    private val layoutEditText: EditText
    private val layoutBottomTextView: TextView
    private val layoutViewModel: CaffeineEditTextLayoutViewModel
    private val binding: CaffeineEditTextLayoutBinding

    var text: String = ""
        set(value) {
            field = value
            layoutEditText.setText(value)
        }
        get() = layoutEditText.text.toString()

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        layoutViewModel = CaffeineEditTextLayoutViewModel(context)
        binding = DataBindingUtil.inflate(inflater, R.layout.caffeine_edit_text_layout, this, true)
        binding.viewModel = layoutViewModel

        layoutEditText = binding.caffeineLayoutEditText
        layoutBottomTextView = binding.caffeineEditTextBottomTextView

        layoutViewModel.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                when (propertyId) {
                    BR.editTextHintColor -> {
                        layoutEditText.setHintTextColor(getColorResource(layoutViewModel.getEditTextHintColor()))
                    }
                    BR.bottomViewTextColor -> {
                        layoutBottomTextView.setTextColor(getColorResource(layoutViewModel.getBottomViewTextColor()))
                    }
                    BR.editTextTextColor -> {
                        layoutEditText.setTextColor(getColorResource(layoutViewModel.getEditTextTextColor()))
                    }
                }
            }
        })

        val attributes = context.theme.obtainStyledAttributes(
            attrs, R.styleable.CaffeineEditTextLayout, 0, 0
        )

        for (i in 0..attributes.indexCount) {
            val attr = attributes.getIndex(i)
            when (attr) {
                R.styleable.CaffeineEditTextLayout_android_imeOptions -> layoutEditText.imeOptions = attributes.getInt(attr, 0)
                R.styleable.CaffeineEditTextLayout_android_inputType -> layoutEditText.inputType = attributes.getInt(attr, 0)
                R.styleable.CaffeineEditTextLayout_android_hint -> layoutViewModel.editTextHint = attributes.getString(attr) ?: ""
                R.styleable.CaffeineEditTextLayout_android_maxLength -> layoutEditText.addFilter(InputFilter.LengthFilter(attributes.getInt(attr, 0)))
                R.styleable.CaffeineEditTextLayout_isDarkMode -> layoutViewModel.setDarkMode(attributes.getBoolean(attr, false))
                R.styleable.CaffeineEditTextLayout_bottomHint -> layoutViewModel.setBottomViewText(attributes.getString(attr), false)
            }
        }

        layoutEditText.setOnFocusChangeListener { _, hasFocus ->
            layoutViewModel.setEditTextIsEmpty(layoutEditText.text.toString().isEmpty())
            layoutViewModel.setEditTextHasFocus(hasFocus)
        }
    }

    fun setOnAction(action: Int, block: () -> Unit) = layoutEditText.setOnAction(action, block)
    fun setOnActionGo(action: () -> Unit) = layoutEditText.setOnActionGo(action)

    fun isEmpty() = layoutEditText.text.isNullOrEmpty()

    fun showError(errorText: String?) = layoutViewModel.setError(errorText)

    fun clearError() = layoutViewModel.clearError()

    fun afterTextChanged(afterTextChanged: (String) -> Unit) {
        this.layoutEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(editable: Editable?) {
                afterTextChanged.invoke(editable.toString())
            }
        })
    }

    private fun getColorResource(@ColorRes color: Int): Int {
        return resources.getColor(color, null)
    }
}