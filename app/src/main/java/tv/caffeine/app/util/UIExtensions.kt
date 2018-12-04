package tv.caffeine.app.util

import android.app.Activity
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import tv.caffeine.app.LobbyDirections
import tv.caffeine.app.R

fun NavController.navigateToLanding(message: String? = null) {
    val action = LobbyDirections.ActionGlobalLandingFragment(message)
    val navOptions = NavOptions.Builder().setPopUpTo(R.id.lobbyFragment, true).build()
    navigate(action, navOptions)
}

fun Activity.dismissKeyboard() {
    currentFocus?.let {
        getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(it.windowToken, 0)
    }
}

