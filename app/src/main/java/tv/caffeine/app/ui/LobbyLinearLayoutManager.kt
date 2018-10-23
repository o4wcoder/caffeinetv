package tv.caffeine.app.ui

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import tv.caffeine.app.R

/**
 * LinearLayoutManager that makes its views 90% of its own width (or height)
 */
class LobbyLinearLayoutManager : LinearLayoutManager {
    private val margin: Int

    @Suppress("Unused")
    @JvmOverloads
    constructor(context: Context?, @RecyclerView.Orientation orientation: Int = RecyclerView.VERTICAL, reverseLayout: Boolean = false) : super(context, orientation, reverseLayout) {
        margin = context?.resources?.getDimension(R.dimen.lobby_card_side_margin)?.toInt() ?: 0
    }

    @Suppress("Unused")
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        margin = context.resources.getDimension(R.dimen.lobby_card_side_margin).toInt()
    }

    override fun generateDefaultLayoutParams() =
            lobbyLayoutSize(super.generateDefaultLayoutParams())

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams?) =
            lobbyLayoutSize(super.generateLayoutParams(lp))

    override fun generateLayoutParams(c: Context?, attrs: AttributeSet?) =
            lobbyLayoutSize(super.generateLayoutParams(c, attrs))

    private fun lobbyLayoutSize(layoutParams: RecyclerView.LayoutParams) =
            layoutParams.apply {
                when(orientation) {
                    HORIZONTAL -> width = horizontalSpace - margin*2
                    VERTICAL -> height = verticalSpace - margin*2
                }
            }

    private val horizontalSpace get() = width - paddingStart - paddingEnd

    private val verticalSpace get() = width - paddingTop - paddingBottom

}
