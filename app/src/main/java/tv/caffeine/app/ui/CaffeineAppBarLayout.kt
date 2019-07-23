package tv.caffeine.app.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import androidx.navigation.NavController
import com.google.android.material.appbar.AppBarLayout
import tv.caffeine.app.R
import tv.caffeine.app.util.safeNavigate

class CaffeineAppBarLayout : AppBarLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    var navController: NavController? = null

    init {
        View.inflate(context, R.layout.caffeine_app_bar, this)
        setButtons()
    }

    private fun setButtons() {
        findViewById<ImageButton>(R.id.releaseSearchButton).setOnClickListener {
            navController?.safeNavigate(R.id.exploreFragment)
        }
        findViewById<ImageButton>(R.id.releaseActivityButton).setOnClickListener {
            navController?.safeNavigate(R.id.notificationsFragment)
        }
    }
}