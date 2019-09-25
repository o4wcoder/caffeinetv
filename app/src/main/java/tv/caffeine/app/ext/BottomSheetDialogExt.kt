package tv.caffeine.app.ext

import android.view.View
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import tv.caffeine.app.R

/**
 * Used for expanding a bottom sheet to full height in landscape mode
 */
fun BottomSheetDialog.expand() {
    setOnShowListener { dialog ->
        val bottomSheet = findViewById<View>(R.id.design_bottom_sheet) as FrameLayout?
        bottomSheet?.let {
            BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
        }
    }
}