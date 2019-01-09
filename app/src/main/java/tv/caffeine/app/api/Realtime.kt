package tv.caffeine.app.api

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.*
import tv.caffeine.app.api.model.Message

interface Realtime {

    @POST("v2/reaper/stages/{stageId}/messages")
    fun sendMessage(@Path("stageId") stageId: String, @Body reaction: Reaction): Deferred<Response<Any>>

    @POST("v2/reaper/messages/{messageId}/endorsements")
    fun endorseMessage(@Path("messageId") messageId: String): Deferred<Response<Unit>>

    @PUT("v4/stage/{username}")
    @Headers("Long-poll: true")
    fun getStage(@Path("username") username: String, @Body body: NewReyes.Message): Deferred<Response<NewReyes.Message>>

    @PUT
    fun connectToStream(@Url url: String, @Body body: NewReyes.ConnectToStream): Deferred<Response<NewReyes.ConnectToStreamResponse>>

    @POST
    fun heartbeat(@Url url: String, @Body body: Any): Deferred<Response<Any>>
}

class IndividualIceCandidate(val candidate: String, @SerializedName("sdpMid") val sdpMid: String, @SerializedName("sdpMLineIndex") val sdpMLineIndex: Int)

class Reaction(val type: String, val publisher: String, val body: Message.Body)

class NewReyes {

    data class Message(val cursor: String? = null, val retryIn: Int? = null, val client: Client? = null, val payload: Payload? = null)

    data class Client(val id: String, val type: String = "android", val headless: Boolean = false, val constrainedBaseline: Boolean = true)

    class Payload(val id: String, val username: String, val title: String, val live: Boolean, val broadcastId: String, val feeds: Map<String, Feed>)

    class Feed(val id: String, val clientId: String, val role: Role, val description: String, val volume: Double, val isHostable: Boolean, val sourceConnectionQuality: String, val content: Content?, val capabilities: Capabilities, val hostableAddress: String, val externalAddress: String, val stream: Stream) {
        enum class Role { primary, secondary }
        class Content(val id: String, val type: Type) {
            enum class Type { game, user }
        }
        class Capabilities(val audio: Boolean, val video: Boolean)
        class Stream(val id: String, val url: String, val sourceId: String, val sdpOffer: String, val sdpAnswer: String)
    }

    class ConnectToStream(val answer: String? = null, val iceCandidates: Array<IndividualIceCandidate>? = null)

    class ConnectToStreamResponse(val iceCandidates: Any?, val id: String, val stageId: String, val urls: ConnectToStreamUrls)

    class ConnectToStreamUrls(val heartbeat: String, val updates: String)

}
