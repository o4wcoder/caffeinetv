package tv.caffeine.app.notifications

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.auth.AuthWatcher
import tv.caffeine.app.repository.DeviceRepository
import tv.caffeine.app.settings.SecureSettingsStorage
import javax.inject.Inject

class NotificationAuthWatcher @Inject constructor(
    private val secureSettingsStorage: SecureSettingsStorage,
    private val scope: CoroutineScope,
    private val deviceRepository: DeviceRepository
) : AuthWatcher {

    override fun onSignIn() {
        Timber.d("NotificationAuthWatcher.onSignIn()")
        secureSettingsStorage.firebaseToken?.let {
            scope.launch {
                Timber.d("Registering device, token $it")
                val result = deviceRepository.registerDevice(it)
                when (result) {
                    is CaffeineResult.Success -> secureSettingsStorage.deviceId = result.value.deviceRegistration?.id
                    is CaffeineResult.Error -> Timber.e("Error creating device on server ${result.error}")
                    is CaffeineResult.Failure -> Timber.e(result.throwable)
                }
            }
        }
    }

    override suspend fun onSignOut(deviceId: String?): CaffeineEmptyResult {
        Timber.d("NotificationAuthWatcher.onSignOut()")
        val id = deviceId ?: return CaffeineEmptyResult.Error(ApiErrorResult(errors = null, reason = "Device Id is null"))
        return deviceRepository.unregisterDevice(id)
    }
}
