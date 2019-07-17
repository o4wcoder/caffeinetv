package tv.caffeine.app.explore

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.test.observeForTesting
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class ExploreViewModelTests {
    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

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
        subject.data.observeForTesting { t ->
            assertTrue(t != null)
            assertTrue(t is CaffeineResult.Success)
            assertTrue((t as CaffeineResult.Success).value is Findings.Explore)
            countDownLatch.countDown()
        }
        subject.queryString = ""
        countDownLatch.await(1, TimeUnit.SECONDS)
    }
}
