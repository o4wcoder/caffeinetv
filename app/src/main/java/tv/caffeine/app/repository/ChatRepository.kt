package tv.caffeine.app.repository

import com.google.gson.Gson
import tv.caffeine.app.api.Reaction
import tv.caffeine.app.api.Realtime
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val realtime: Realtime,
    private val gson: Gson
) {

    suspend fun sendMessage(stageId: String, reaction: Reaction) =
        realtime.sendMessage(stageId, reaction).awaitAndParseErrors(gson)

    suspend fun endorseMessage(messageId: String) =
        realtime.endorseMessage(messageId).awaitEmptyAndParseErrors(gson)
}