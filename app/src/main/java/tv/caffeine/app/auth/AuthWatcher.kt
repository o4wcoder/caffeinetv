package tv.caffeine.app.auth

interface AuthWatcher {
    fun onSignIn()
    fun onSignOut()
}
