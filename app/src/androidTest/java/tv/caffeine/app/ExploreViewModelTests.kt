package tv.caffeine.app

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.explore.ExploreViewModel
import tv.caffeine.app.explore.FindBroadcastersUseCase
import tv.caffeine.app.explore.Findings
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ExploreViewModelTests {
    private lateinit var subject: ExploreViewModel
    @Mock
    lateinit var observer: Observer<CaffeineResult<Findings>>
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
//        mockkStatic("android.os.Looper")
//        every { Looper.getMainLooper() } returns mock<Looper>()
//        mockkStatic("androidx.arch.core.executor.ArchTaskExecutor")
//        every { ArchTaskExecutor.getInstance().isMainThread() } returns true
        val result: CaffeineResult<Findings> = CaffeineResult.Success(Findings.Explore(arrayOf()))
        val findBroadcastersUseCase = mockk<FindBroadcastersUseCase>()
        coEvery { findBroadcastersUseCase.invoke(any()) } returns result
        subject = ExploreViewModel(findBroadcastersUseCase)
    }

    @Test
    fun emptySearchStringReturnsExploreResult() {
        val countDownLatch = CountDownLatch(1)
        subject.data.observeForever { t ->
            Assert.assertTrue(t != null)
            Assert.assertTrue(t is CaffeineResult.Success)
            Assert.assertTrue((t as CaffeineResult.Success).value is Findings.Explore)
            countDownLatch.countDown()
        }
        subject.queryString = ""
        countDownLatch.await(1, TimeUnit.SECONDS)
    }
}
