package tv.caffeine.app.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
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
            showNewActivityIcon(false)
            navController?.safeNavigate(R.id.notificationsFragment)
        }
    }

    fun setWordmarkLongClickListener(onLongClick: (View) -> Unit) {
        findViewById<ImageView>(R.id.caffeine_wordmark).setOnLongClickListener {
            onLongClick(it)
            true
        }
    }

    fun showNewActivityIcon(shouldShow: Boolean) {
        val image = if (shouldShow) R.drawable.ic_activity else R.drawable.ic_no_activity
        findViewById<ImageButton>(R.id.releaseActivityButton).setImageResource(image)
    }
}