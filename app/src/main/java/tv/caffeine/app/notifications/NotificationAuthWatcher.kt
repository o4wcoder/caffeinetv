package tv.caffeine.app.notifications

import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.DeviceRegistration
import tv.caffeine.app.api.DevicesService
import tv.caffeine.app.api.RegisterDeviceBody
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.auth.AuthWatcher
import javax.inject.Inject

class NotificationAuthWatcher @Inject constructor(
    private val devicesService: DevicesService,
    private val firebaseInstanceId: FirebaseInstanceId,
    private val gson: Gson
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
                        devicesService
                                .registerDevice(body)
                                .awaitAndParseErrors(gson)
                    }
                }
            }
        }
    }

    override fun onSignOut() {
        Timber.d("NotificationAuthWatcher.onSignOut()")
        firebaseInstanceId.instanceId.addOnCompleteListener { task ->
            coroutineScope.launch {
                if (task.isSuccessful) {
                    task.result?.let { instanceId ->
                        Timber.d("Unregistering device, token ${instanceId.token}")
                        devicesService
                                .unregisterDevice(instanceId.token)
                                .awaitAndParseErrors(gson)
                    }
                }
            }
        }
    }
}
