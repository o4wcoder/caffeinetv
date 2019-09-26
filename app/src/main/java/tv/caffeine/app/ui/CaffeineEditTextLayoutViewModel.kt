package tv.caffeine.app.ui

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import androidx.annotation.ColorRes
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry
import tv.caffeine.app.BR
import tv.caffeine.app.R

class CaffeineEditTextLayoutViewModel(val context: Context) : BaseObservable() {

    private val callbacks: PropertyChangeRegistry = PropertyChangeRegistry()

    override fun addOnPropertyChangedCallback(
        callback: Observable.OnPropertyChangedCallback
    ) { callbacks.add(callback) }

    private val defaultStateLineColorPair = Pair(R.color.caffeine_edit_text_layout_line_dark, R.color.caffeine_edit_text_layout_line)
    private val finishedStateLineColorPair = Pair(R.color.caffeine_edit_text_layout_line_finished_dark, R.color.caffeine_edit_text_layout_line_finished)
    private val focusedStateLineColorPair = Pair(R.color.caffeine_edit_text_layout_line_focused_dark, R.color.caffeine_edit_text_layout_line_focused)
    private val editTextTextColorPair = Pair(R.color.caffeine_edit_text_layout_text_dark, R.color.caffeine_edit_text_layout_text)
    private val bottomViewTextColorPair = Pair(R.color.caffeine_edit_text_layout_bottom_hint_dark, R.color.caffeine_edit_text_layout_bottom_hint)

    private var isDarkMode = false
    private var editTextHasFocus = false
    private var editTextIsEmpty = true
    private var isError = false
    private var shouldShowBottomTextView = false

    private var editTextHint: String = ""
    private var bottomViewText: String? = null

    @Bindable
    fun getBottomViewVisibility() = if (shouldShowBottomTextView) View.VISIBLE else View.GONE

    @Bindable
    fun getEditTextHint() = editTextHint

    @Bindable
    @ColorRes
    fun getEditTextHintColor() = R.color.caffeine_edit_text_layout_main_hint

    @Bindable
    @ColorRes
    fun getEditTextTextColor() = getColorForMode(editTextTextColorPair)

    @Bindable
    fun getLineColor(): ColorStateList {
        val color =
            if (isError) {
                R.color.caffeine_edit_text_layout_error
            } else {
                when {
                    editTextHasFocus -> getColorForMode(focusedStateLineColorPair)
                    editTextIsEmpty -> getColorForMode(defaultStateLineColorPair)
                    else -> getColorForMode(finishedStateLineColorPair)
                }
            }

        return getColorStateList(color)
    }

    @Bindable
    @ColorRes
    fun getBottomViewTextColor(): Int {
        return if (isError) {
            R.color.caffeine_edit_text_layout_error
        } else {
            getColorForMode(bottomViewTextColorPair)
        }
    }

    @Bindable
    fun getBottomViewText() = bottomViewText?.let { it } ?: editTextHint

    fun setEditTextHint(text: String) {
        editTextHint = text
        notifyPropertyChanged(BR.editTextHint)
    }

    fun setBottomViewText(text: String?, isError: Boolean) {
        bottomViewText = text
        this.isError = isError
        setBottomViewVisibility(true)
        notifyPropertyChanged(BR.bottomViewText)
        notifyPropertyChanged(BR.bottomViewTextColor)
        notifyPropertyChanged(BR.bottomViewVisibility)
        notifyPropertyChanged(BR.lineColor)
    }

    fun setEditTextHasFocus(hasFocus: Boolean) {
        editTextHasFocus = hasFocus
        setBottomViewVisibility(hasFocus || isError)
        notifyPropertyChanged(BR.bottomViewVisibility)
        notifyPropertyChanged(BR.lineColor)
    }

    fun setEditTextIsEmpty(isEmpty: Boolean) {
        editTextIsEmpty = isEmpty
        notifyPropertyChanged(BR.lineColor)
    }

    fun setDarkMode(isDarkMode: Boolean) {
        this.isDarkMode = isDarkMode
        notifyPropertyChanged(BR.lineColor)
        notifyPropertyChanged(BR.editTextHintColor)
        notifyPropertyChanged(BR.bottomViewTextColor)
        notifyPropertyChanged(BR.editTextTextColor)
    }

    fun setError(errorMessage: String) {
        setBottomViewText(errorMessage, true)
    }

    fun clearError() {
        setBottomViewText(null, false)
    }

    private fun setBottomViewVisibility(shouldShow: Boolean) {
        shouldShowBottomTextView = shouldShow
        notifyPropertyChanged(BR.bottomViewVisibility)
    }

    private fun getColorResource(@ColorRes color: Int): Int {
        return context.resources.getColor(color, null)
    }

    private fun getColorStateList(@ColorRes color: Int): ColorStateList {
        return ColorStateList.valueOf(getColorResource(color))
    }

    @ColorRes
    private fun getColorForMode(pair: Pair<Int, Int>): Int {
        return if (isDarkMode) pair.first else pair.second
    }

    override fun notifyPropertyChanged(fieldId: Int) {
        super.notifyPropertyChanged(fieldId)
        callbacks.notifyCallbacks(this, fieldId, null)
    }
}