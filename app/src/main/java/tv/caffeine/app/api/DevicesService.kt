package tv.caffeine.app.api

import android.os.Build
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import tv.caffeine.app.BuildConfig

interface DevicesService {
    @POST("v1/devices")
    fun registerDevice(@Body body: RegisterDeviceBody): Deferred<Response<Device>>

    @DELETE("v1/devices/{deviceId}")
    fun unregisterDevice(@Path("deviceId") deviceId: String): Deferred<Response<Any>>

    @PATCH("v1/devices/{deviceId}")
    fun updateDevice(@Path("deviceId") deviceId: String, @Body body: RegisterDeviceBody): Deferred<Response<Device>>
}

class RegisterDeviceBody(val device: DeviceRegistration)

// TODO https://github.com/jaredrummler/AndroidDeviceNames
class DeviceRegistration(
    val id: String? = null,
    val notificationToken: String,
    val os: String = "ANDROID",
    val systemVersion: String = "${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})",
//        val name: String = BluetoothAdapter.getDefaultAdapter().name ?: "Unknown",
    val name: String = "TODO",
    val model: String = "${Build.MANUFACTURER}, ${Build.MODEL}, ${Build.BRAND}, ${Build.PRODUCT}",
    val platform: String = "${Build.HARDWARE} (${Build.BOARD})",
    val appVersionRelease: String = BuildConfig.VERSION_NAME,
    val appVersionBuild: String = BuildConfig.VERSION_CODE.toString(),
    val certificate: String = if (BuildConfig.DEBUG) "Development" else "Production"
)

data class Device(
    @SerializedName("device") val deviceRegistration: DeviceRegistration? = null
)