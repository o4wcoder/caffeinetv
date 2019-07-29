package tv.caffeine.app.util

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.gson.Gson
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tv.caffeine.app.api.Device
import tv.caffeine.app.api.DevicesService
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import tv.caffeine.app.repository.DeviceRepository

class DevicesRepositoryTests {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val coroutinesTestRule = CoroutinesTestRule()

    @MockK private lateinit var fakeDevicesService: DevicesService
    @MockK private lateinit var fakeGson: Gson
    @MockK private lateinit var fakeDevice: Device

    private lateinit var subject: DeviceRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        val deviceResult = CaffeineResult.Success(fakeDevice)
        val emptyResult = CaffeineEmptyResult.Success
        coEvery { fakeDevicesService.registerDevice(any()).awaitAndParseErrors(fakeGson) } returns deviceResult
        coEvery { fakeDevicesService.unregisterDevice(any()).awaitEmptyAndParseErrors(fakeGson) } returns emptyResult
        coEvery { fakeDevicesService.updateDevice(any(), any()).awaitAndParseErrors(fakeGson) } returns deviceResult

        subject = DeviceRepository(fakeDevicesService, fakeGson)
    }

    @Test
    fun `devices service register device is called on register device`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        subject.registerDevice("123")
        coVerify(exactly = 1) { fakeDevicesService.registerDevice(any()) }
    }

    @Test
    fun `devices service unregister device is called on unregister device`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        subject.unregisterDevice("123")
        coVerify(exactly = 1) { fakeDevicesService.unregisterDevice(any()) }
    }

    @Test
    fun `devices service update device is called on update device`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        subject.updateDevice("123", "123")
        coVerify(exactly = 1) { fakeDevicesService.updateDevice(any(), any()) }
    }
}