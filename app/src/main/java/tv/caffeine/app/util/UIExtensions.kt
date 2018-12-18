package tv.caffeine.app.util

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.net.ConnectivityManager
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import tv.caffeine.app.R
import tv.caffeine.app.ui.CaffeineFragment

fun Context.dismissKeyboard(view: View) {
    getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService<ConnectivityManager>() ?: return false
    return connectivityManager.activeNetworkInfo?.isConnected == true
}

fun Activity.dismissKeyboard() {
    currentFocus?.let { dismissKeyboard(it) }
}

fun Activity.setImmersiveSticky() {
    window.apply {
        // Prevent white status/nav bars when launching dialogs and toggling the immersive mode at the same time.
        addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
        clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        decorView.systemUiVisibility = decorView.systemUiVisibility
                .or(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                .or(View.SYSTEM_UI_FLAG_FULLSCREEN)
                .or(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }
}

fun Activity.unsetImmersiveSticky() {
    window.apply {
        // Prevent white status/nav bars when launching dialogs and toggling the immersive mode at the same time.
        clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
        addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        decorView.systemUiVisibility = decorView.systemUiVisibility
                .and(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY.inv())
                .and(View.SYSTEM_UI_FLAG_FULLSCREEN.inv())
                .and(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION.inv())
    }
}

fun Activity.setDarkMode(isDarkMode: Boolean) {
    window.apply {
        addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        statusBarColor = ContextCompat.getColor(context,
                if (isDarkMode) android.R.color.black else R.color.status_bar)
        if (isDarkMode) {
            decorView.systemUiVisibility = decorView.systemUiVisibility
                    .and(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv())
        } else {
            decorView.systemUiVisibility = decorView.systemUiVisibility
                    .or(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            navigationBarColor = ContextCompat.getColor(context,
                    if (isDarkMode) android.R.color.black else R.color.nav_bar)
            if (isDarkMode) {
                decorView.systemUiVisibility = decorView.systemUiVisibility
                        .and(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv())
            } else {
                decorView.systemUiVisibility = decorView.systemUiVisibility
                        .or(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
            }
        }
    }
}

fun Activity.showSnackbar(@StringRes resId: Int) {
    Snackbar.make(window.decorView, resId, Snackbar.LENGTH_SHORT).show()
}

fun CaffeineFragment.showSnackbar(@StringRes resId: Int) {
    val view = this.view ?: return
    Snackbar.make(view, resId, Snackbar.LENGTH_SHORT).show()
}

fun convertLinks(
        @StringRes stringResId: Int,
        resources: Resources,
        spanFactory: (url: String?) -> URLSpan
): Spannable {
    val spannable = SpannableString(HtmlCompat.fromHtml(
            resources.getString(stringResId), HtmlCompat.FROM_HTML_MODE_LEGACY))
    for (urlSpan in spannable.getSpans<URLSpan>(0, spannable.length, URLSpan::class.java)) {
        spannable.setSpan(spanFactory(urlSpan.url),
                spannable.getSpanStart(urlSpan),
                spannable.getSpanEnd(urlSpan),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.removeSpan(urlSpan)
    }
    return spannable
}

fun RecyclerView.clearItemDecoration() {
    for (i in 0 until itemDecorationCount) {
        removeItemDecorationAt(i)
    }
}

fun RecyclerView.setItemDecoration(itemDecoration: RecyclerView.ItemDecoration) {
    clearItemDecoration()
    addItemDecoration(itemDecoration)
}
