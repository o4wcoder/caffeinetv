package tv.caffeine.app.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView

/**
 * The standard {@link TextView} does not ellipsize unless the maxLines is set.
 * Even when it is set, under large fonts, the text can be cut before it reaches maxLines.
 * This class automatically calculates the visible lines and set maxLines to it so it always ellipsizes correctly.
 */
class AutoEllipsizeTextView : TextView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        viewTreeObserver.addOnGlobalLayoutListener {
            maxLines = height / lineHeight
        }
    }
}