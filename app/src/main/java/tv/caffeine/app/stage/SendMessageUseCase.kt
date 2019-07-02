package tv.caffeine.app.stage

import com.google.gson.Gson
import tv.caffeine.app.api.Reaction
import tv.caffeine.app.api.Realtime
import tv.caffeine.app.api.model.awaitAndParseErrors
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val realtime: Realtime,
    private val gson: Gson
) {
    suspend operator fun invoke(stageId: String, reaction: Reaction) =
        realtime.sendMessage(stageId, reaction).awaitAndParseErrors(gson)
}
