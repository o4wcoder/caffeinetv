package tv.caffeine.app.stage

import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tv.caffeine.app.api.NewReyes
import tv.caffeine.app.api.Realtime
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.api.model.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject

interface StageDirector {
    @ExperimentalCoroutinesApi
    suspend fun stageConfiguration(username: String, clientId: String): Flow<CaffeineResult<Map<String, NewReyes.Feed>>>
}

private const val DEFAULT_RETRY_DELAY_SECONDS = 10L

class ClassicStageDirector @Inject constructor(
    private val getStageUseCase: ClassicGetStageUseCase
) : StageDirector {

    @ExperimentalCoroutinesApi
    override suspend fun stageConfiguration(username: String, clientId: String) = flow {
        var message = NewReyes.Message(client = NewReyes.Client(id = clientId))
        do {
            var retryIn: Long? = null
            val result = getStageUseCase(username, message)
            emit(result.map { it.payload?.feeds ?: mapOf() })
            if (result is CaffeineResult.Success) {
                message = result.value.copy()
                retryIn = message.retryIn?.toLong()
            }
            delay(TimeUnit.SECONDS.toMillis(retryIn ?: DEFAULT_RETRY_DELAY_SECONDS))
        } while (shouldContinue(result))
    }

    private fun shouldContinue(result: CaffeineResult<NewReyes.Message>): Boolean {
        return (result !is CaffeineResult.Failure || result.throwable !is CancellationException)
    }
}

class ClassicGetStageUseCase @Inject constructor(
    private val realtime: Realtime,
    private val gson: Gson
) {
    suspend operator fun invoke(username: String, message: NewReyes.Message): CaffeineResult<NewReyes.Message> =
        realtime.getStage(username, message).awaitAndParseErrors(gson)
}
