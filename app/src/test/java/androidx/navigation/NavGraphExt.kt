package androidx.navigation

import android.net.Uri

fun NavGraph.findDestination(uri: Uri): NavDestination? {
    return matchDeepLink(uri)?.destination
}
