package tv.caffeine.app.stage

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.util.TestCoroutineContext

class ClassicStageDirectorTests {
    lateinit var subject: ClassicStageDirector
    @MockK lateinit var getStageUseCase: ClassicGetStageUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        subject = ClassicStageDirector(getStageUseCase)
    }

    @FlowPreview
    @InternalCoroutinesApi
    @Test
    fun `errors are propagated`() {
        val error = ApiErrorResult(null)
        coEvery { getStageUseCase(any(), any()) } returns CaffeineResult.Error(error)
        runBlocking(TestCoroutineContext()) {
            subject.stageConfiguration("username", "clientId").take(5).collect { result ->
                when (result) {
                    is CaffeineResult.Error -> assertEquals(error, result.error)
                    else -> fail("Expected the error result")
                }
            }
        }
        coVerify(exactly = 5) { getStageUseCase(any(), any()) }
    }
}
