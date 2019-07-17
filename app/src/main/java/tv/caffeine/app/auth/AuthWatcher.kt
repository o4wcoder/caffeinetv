package tv.caffeine.app.auth

import tv.caffeine.app.api.model.CaffeineEmptyResult

interface AuthWatcher {
    fun onSignIn()
    suspend fun onSignOut(deviceId: String?): CaffeineEmptyResult
}
