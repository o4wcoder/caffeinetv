package tv.caffeine.app.api

import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
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
