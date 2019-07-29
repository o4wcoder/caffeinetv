
package tv.caffeine.app.util

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tv.caffeine.app.api.Device
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.notifications.NotificationAuthWatcher
import tv.caffeine.app.repository.DeviceRepository
import tv.caffeine.app.settings.SecureSettingsStorage

class NotificationAuthWatcherTests {
    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()
    @get:Rule val coroutinesTestRule = CoroutinesTestRule()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    @MockK private lateinit var fakeSecureSettingsStorageModule: SecureSettingsStorage
    @MockK private lateinit var fakeDevice: Device
    @MockK private lateinit var fakeDeviceRepository: DeviceRepository
    val fakeDeviceId = "fake_device_id"

    private lateinit var subject: NotificationAuthWatcher

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        val emptyResult = CaffeineEmptyResult.Success
        coEvery { fakeDeviceRepository.unregisterDevice(any()) } returns emptyResult
        val result: CaffeineResult<Device> = CaffeineResult.Success(fakeDevice)
        coEvery { fakeDevice.deviceRegistration?.id } returns fakeDeviceId
        coEvery { fakeDeviceRepository.registerDevice(any()) } returns result
        coEvery { fakeSecureSettingsStorageModule.firebaseToken } returns "123"
        coEvery { fakeSecureSettingsStorageModule.deviceId = any() } just runs

        subject = NotificationAuthWatcher(fakeSecureSettingsStorageModule, coroutineScope, fakeDeviceRepository)
    }

    @Test
    fun `register device is called on sign in `() {
        subject.onSignIn()
        coVerify(exactly = 1) { fakeDeviceRepository.registerDevice(any()) }
    }

    @Test
    fun `device id is set in storage on sign in `() {
        subject.onSignIn()
        coVerify(exactly = 1) { fakeSecureSettingsStorageModule.deviceId = any() }
    }

    @Test
    fun `unregister device is not called on sign out with null device id`() = runBlockingTest {
        val id = null
        subject.onSignOut(id)
        coVerify(exactly = 0) { fakeDeviceRepository.unregisterDevice(any()) }
    }

    @Test
    fun `unregister device is called on sign out with valid device id`() = runBlockingTest {
        val id = "123"
        subject.onSignOut(id)
        coVerify(exactly = 1) { fakeDeviceRepository.unregisterDevice(any()) }
    }
}