package tv.caffeine.app.stage

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import timber.log.Timber
import tv.caffeine.app.realtime.WebSocketController

class MessageHandshake(private val accessToken: String, private val xCredential: String) {
    private val webSocketController = WebSocketController("msg")
    private val gson: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

    class Message(val publisher: Publisher, val id: String, val type: String, val body: Body, val endorsementCount: Int = 0)
    class Publisher(val abilities: Map<String, Boolean>, val age: Int?, val avatarImagePath: String, val bio: String, val broadcastId: String, val caid: String, val countryCode: String, val countryName: String, val followersCount: Int, val followingCount: Int, val gender: String, val isFeatured: Boolean, val isOnline: Boolean, val isVerified: Boolean, val name: String?, val stageId: String, val username: String)
    class Body(val text: String)

    fun connect(stageIdentifier: String, callback: (Message) -> Unit) {
        val url = "wss://realtime.caffeine.tv/v2/reaper/stages/$stageIdentifier/messages"
        val headers = """{
                "Headers": {
                    "x-credential" : "$xCredential",
                    "authorization" : "Bearer $accessToken",
                    "X-Client-Type" : "android",
                    "X-Client-Version" : "0"
                }
            }""".trimMargin()
        webSocketController.connect(url, headers) {
            Timber.d("Received message $it")
            val message = gson.fromJson(it, Message::class.java)
            callback(message)
        }
    }

    fun close() {
        Timber.d("msg - closing handshake handler")
        webSocketController.close()
    }

}