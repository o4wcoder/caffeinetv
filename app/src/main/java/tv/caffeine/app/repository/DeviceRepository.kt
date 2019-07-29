package tv.caffeine.app.repository

import com.google.gson.Gson
import tv.caffeine.app.api.Device
import tv.caffeine.app.api.DeviceRegistration
import tv.caffeine.app.api.DevicesService
import tv.caffeine.app.api.RegisterDeviceBody
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val devicesService: DevicesService,
    private val gson: Gson
) {
    suspend fun registerDevice(notificationToken: String): CaffeineResult<Device> {
        val body = RegisterDeviceBody(DeviceRegistration(notificationToken = notificationToken))
        return devicesService
            .registerDevice(body)
            .awaitAndParseErrors(gson)
    }

    suspend fun unregisterDevice(deviceId: String): CaffeineEmptyResult {
        return devicesService.unregisterDevice(deviceId).awaitEmptyAndParseErrors(gson)
    }

    suspend fun updateDevice(deviceId: String, notificationToken: String): CaffeineResult<Device> {
        val body = RegisterDeviceBody(DeviceRegistration(notificationToken = notificationToken))
        return devicesService.updateDevice(deviceId, body).awaitAndParseErrors(gson)
    }
}