package tv.caffeine.app.stage

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import timber.log.Timber
import tv.caffeine.app.api.Api
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.realtime.WebSocketController
import javax.inject.Inject

class MessageHandshake @Inject constructor(
        private val tokenStore: TokenStore
) {
    private val webSocketController = WebSocketController("msg")
    private val gson: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

    fun connect(stageIdentifier: String, callback: (Api.Message) -> Unit) {
        val url = "wss://realtime.caffeine.tv/v2/reaper/stages/$stageIdentifier/messages"
        val headers = tokenStore.webSocketHeader()
        webSocketController.open(url, headers) {
            Timber.d("Received message $it")
            val message = gson.fromJson(it, Api.Message::class.java)
            callback(message)
        }
    }

    fun close() {
        Timber.d("msg - closing handshake handler")
        webSocketController.close()
    }

}
