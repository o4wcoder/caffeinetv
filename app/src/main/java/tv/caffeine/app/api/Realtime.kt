package tv.caffeine.app.api

import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.*
import tv.caffeine.app.api.model.Message

interface Realtime {
    @POST("v2/broadcasts/streams/{streamId}/viewers")
    fun createViewer(@Path("streamId") streamId: String, @Body body: CreateViewerBody = CreateViewerBody(true)): Deferred<Response<CreateViewerResult>>

    @POST("v2/broadcasts/streams")
    fun initializeStream(@Body initBody: StreamInitBody): Deferred<Response<StreamInitResult>>

    @PUT("v2/broadcasts/viewers/{viewerId}")
    fun sendIceCandidate(@Path("viewerId") viewerId: String, @Body iceCandidates: IceCandidatesBody): Deferred<Response<Any>>

    @PUT("v2/broadcasts/viewers/{viewerId}")
    fun sendAnswer(@Path("viewerId") viewerId: String, @Body answer: AnswerBody): Deferred<Response<Any>>

    @POST("v2/broadcasts/viewers/{viewerId}/heartbeat")
    fun sendHeartbeat(@Path("viewerId") viewerId: String, @Body heartbeatBody: HeartbeatBody): Deferred<Response<Any>>

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

class CreateViewerBody(val constrained_baseline: Boolean)

class CreateViewerResult(val id: String, val offer: String, val signed_payload: String)

class StreamInitBody(val stage_id: String, val offer: String)
class StreamInitResult(val id: String, val answer: String, val signed_payload: String)

class IceCandidatesBody(val ice_candidates: Array<IndividualIceCandidate>, val signed_payload: String)
class AnswerBody(val answer: String, val signed_payload: String)

class IndividualIceCandidate(val candidate: String, val sdpMid: String, val sdpMLineIndex: Int)

class HeartbeatBody(val signed_payload: String)

class Reaction(val type: String, val publisher: String, val body: Message.Body)

class NewReyes {

    data class Message(val cursor: String? = null, val retry_in: Int? = null, val client: Client? = null, val payload: Payload? = null)

    data class Client(val id: String, val type: String = "android", val headless: Boolean = false, val constrained_baseline: Boolean = true)

    class Payload(val id: String, val username: String, val title: String, val live: Boolean, val broadcast_id: String, val feeds: Map<String, Feed>)

    class Feed(val id: String, val client_id: String, val role: Role, val description: String, val volume: Double, val is_hostable: Boolean, val source_connection_quality: String, val content: Content?, val capabilities: Capabilities, val hostable_address: String, val external_address: String, val stream: Stream) {
        enum class Role { primary, secondary }
        class Content(val id: String, val type: Type) {
            enum class Type { game, user }
        }
        class Capabilities(val audio: Boolean, val video: Boolean)
        class Stream(val id: String, val url: String, val source_id: String, val sdp_offer: String, val sdp_answer: String)
    }

    class ConnectToStream(val answer: String? = null, val ice_candidates: Array<IndividualIceCandidate>? = null)

    class ConnectToStreamResponse(val ice_candidates: Any?, val id: String, val stage_id: String, val urls: ConnectToStreamUrls)

    class ConnectToStreamUrls(val heartbeat: String, val updates: String)

}
