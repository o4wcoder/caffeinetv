package tv.caffeine.app.stage

import com.google.gson.Gson
import tv.caffeine.app.api.Realtime
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import javax.inject.Inject

class EndorseMessageUseCase @Inject constructor(
    private val realtime: Realtime,
    private val gson: Gson
) {
    suspend operator fun invoke(messageId: String) =
        realtime.endorseMessage(messageId).awaitEmptyAndParseErrors(gson)
}
