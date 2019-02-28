package tv.caffeine.app.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class PaddingItemDecoration(val paddingLeft: Int, val paddingTop: Int, val paddingRight: Int, val paddingBottom: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.left = paddingLeft
        outRect.right = paddingRight
        outRect.top = paddingTop
        outRect.bottom = paddingBottom
    }
}
