package tv.caffeine.app.stage

import com.apollographql.apollo.api.Response
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.caffeine.app.api.isOutOfCapacityError
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.stream.StageSubscription
import tv.caffeine.app.stream.type.ContentRating

class GraphqlStageDirectorTest {

    @MockK lateinit var response: Response<StageSubscription.Data>

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `stage Reyes v5 response with an out-of-capacity error is converted to OutOfCapacityError`() {
        val stage = StageSubscription.Stage1("", "id", "username", "title", "broadcast_id", ContentRating.EVERYONE, true, listOf())
        val data = StageSubscription.Data(StageSubscription.Stage(
            "",
            StageSubscription.Error("OutOfCapacityError", "error", "message"),
            stage))
        every { response.data() } returns data
        every { response.errors() } returns listOf()
        val result = response.asCaffeineResult()
        assertTrue(result is CaffeineResult.Error)
        assertTrue((result as CaffeineResult.Error).error.isOutOfCapacityError())
    }

    @Test
    fun `stage Reyes v5 response without an out-of-capacity error is converted to a successful result`() {
        val stage = StageSubscription.Stage1("", "id", "username", "title", "broadcast_id", ContentRating.EVERYONE, true, listOf())
        val data = StageSubscription.Data(StageSubscription.Stage("", null, stage))
        every { response.data() } returns data
        every { response.errors() } returns listOf()
        val result = response.asCaffeineResult()
        assertTrue(result is CaffeineResult.Success)
        assertEquals((result as CaffeineResult.Success).value, data)
    }
}