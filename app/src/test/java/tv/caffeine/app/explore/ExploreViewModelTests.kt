package tv.caffeine.app.explore

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.api.model.CaffeineResult
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class ExploreViewModelTests {
    @Rule @JvmField val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var subject: ExploreViewModel

    @Before
    fun setup() {
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
