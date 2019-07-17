package tv.caffeine.app.notifications

import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.DeviceRegistration
import tv.caffeine.app.api.DevicesService
import tv.caffeine.app.api.RegisterDeviceBody
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import tv.caffeine.app.auth.AuthWatcher
import tv.caffeine.app.settings.SecureSettingsStorage
import javax.inject.Inject

class NotificationAuthWatcher @Inject constructor(
    private val devicesService: DevicesService,
    private val firebaseInstanceId: FirebaseInstanceId,
    private val gson: Gson,
    private val secureSettingsStorage: SecureSettingsStorage
) : AuthWatcher {
    private val coroutineScope: CoroutineScope = GlobalScope

    override fun onSignIn() {
        Timber.d("NotificationAuthWatcher.onSignIn()")
        firebaseInstanceId.instanceId.addOnCompleteListener { task ->
            coroutineScope.launch {
                if (task.isSuccessful) {
                    task.result?.let { instanceId ->
                        val body = RegisterDeviceBody(DeviceRegistration(notificationToken = instanceId.token))
                        Timber.d("Registering device, token ${instanceId.token}")
                        val result = devicesService
                                .registerDevice(body)
                                .awaitAndParseErrors(gson)
                        when (result) {
                            is CaffeineResult.Success -> secureSettingsStorage.deviceId = result.value.deviceRegistration?.id
                            is CaffeineResult.Error -> Timber.e("Error creating device on server ${result.error}")
                            is CaffeineResult.Failure -> Timber.e(result.throwable)
                        }
                    }
                }
            }
        }
    }

    override suspend fun onSignOut(deviceId: String?): CaffeineEmptyResult {
        Timber.d("NotificationAuthWatcher.onSignOut()")
        val id = deviceId ?: return CaffeineEmptyResult.Error(ApiErrorResult(errors = null, reason = "Device Id is null"))
        return devicesService.unregisterDevice(id).awaitEmptyAndParseErrors(gson)
    }
}
