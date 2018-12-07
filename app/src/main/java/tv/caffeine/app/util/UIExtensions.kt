package tv.caffeine.app.util

import android.app.Activity
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.google.android.material.snackbar.Snackbar
import tv.caffeine.app.LobbyDirections
import tv.caffeine.app.R
import tv.caffeine.app.profile.ReportOrIgnoreDialogFragment
import tv.caffeine.app.ui.CaffeineFragment

fun NavController.navigateToLanding(message: String? = null) {
    val action = LobbyDirections.ActionGlobalLandingFragment(message)
    val navOptions = NavOptions.Builder().setPopUpTo(R.id.lobbyFragment, true).build()
    navigate(action, navOptions)
}

fun FragmentManager.navigateToReportOrIgnoreDialog(caid: String, username: String, shouldNavigateBackWhenDone: Boolean) {
    ReportOrIgnoreDialogFragment().let {
        it.arguments = LobbyDirections.ActionGlobalReportOrIgnoreDialogFragment(
                caid, username, shouldNavigateBackWhenDone).arguments
        it.show(this, "reportOrIgnoreUser")
    }
}

fun Activity.dismissKeyboard() {
    currentFocus?.let {
        getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(it.windowToken, 0)
    }
}

fun Activity.setImmersiveSticky() {
    window.decorView.let {
        it.systemUiVisibility = it.systemUiVisibility
                .or(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                .or(View.SYSTEM_UI_FLAG_FULLSCREEN)
                .or(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }
}

fun Activity.unsetImmersiveSticky() {
    window.decorView.let {
        it.systemUiVisibility = it.systemUiVisibility
                .and(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY.inv())
                .and(View.SYSTEM_UI_FLAG_FULLSCREEN.inv())
                .and(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION.inv())
    }
}

fun Activity.showSnackbar(@StringRes resId: Int) {
    Snackbar.make(window.decorView, resId, Snackbar.LENGTH_SHORT).show()
}

fun CaffeineFragment.showSnackbar(@StringRes resId: Int) {
    val view = this.view ?: return
    Snackbar.make(view, resId, Snackbar.LENGTH_SHORT).show()
}
