package tv.caffeine.app.ui

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import tv.caffeine.app.R

private const val DEFAULT_ROW_COUNT = 2

/**
 * GridLayoutManager that makes the items fit and cover the full width
 */
class ChatLayoutManager : GridLayoutManager {
    private val rowCount: Int

    @Suppress("Unused")
    @JvmOverloads
    constructor(context: Context?, spanCount: Int, orientation: Int = RecyclerView.VERTICAL, reverseLayout: Boolean = false) :
            super(context, spanCount, orientation, reverseLayout) {
        rowCount = context?.resources?.getInteger(R.integer.chat_row_count) ?: DEFAULT_ROW_COUNT
    }

    @Suppress("Unused")
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttr, defStyleRes) {
        rowCount = context.resources.getInteger(R.integer.chat_row_count)
    }

    override fun generateDefaultLayoutParams() =
            chatLayoutSize(super.generateDefaultLayoutParams())

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams?) =
            chatLayoutSize(super.generateLayoutParams(lp))

    override fun generateLayoutParams(c: Context?, attrs: AttributeSet?) =
            chatLayoutSize(super.generateLayoutParams(c, attrs))

    private fun chatLayoutSize(layoutParams: RecyclerView.LayoutParams) =
            layoutParams.apply {
                width = horizontalSpace / spanCount
                height = verticalSpace / rowCount
            }

    private val horizontalSpace get() = width - paddingStart - paddingEnd

    private val verticalSpace get() = height - paddingTop - paddingBottom
}
