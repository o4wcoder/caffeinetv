package tv.caffeine.app.realtime

import retrofit2.Call
import retrofit2.http.*

interface Realtime {
    @POST("v2/broadcasts/streams/{streamId}/viewers")
    fun createViewer(@Header("Authorization") authorization: String, @Header("x-credential") xCredential: String, @Path("streamId") streamId: String): Call<CreateViewerResult>

    @POST("v2/broadcasts/streams")
    fun initializeStream(@Header("Authorization") authorization: String, @Header("x-credential") xCredential: String, @Body initBody: StreamInitBody): Call<StreamInitResult>

    @PUT("v2/broadcasts/viewers/{viewerId}")
    fun sendIceCandidate(@Header("Authorization") authorization: String, @Header("x-credential") xCredential: String, @Body iceCandidates: IceCandidatesBody, @Path("viewerId") viewerId: String): Call<Void>

    @PUT("v2/broadcasts/viewers/{viewerId}")
    fun sendAnswer(@Header("Authorization") authorization: String, @Header("x-credential") xCredential: String, @Body answer: AnswerBody, @Path("viewerId") viewerId: String): Call<Void>

    @POST("v2/broadcasts/viewers/{viewerId}/heartbeat")
    fun sendHeartbeat(@Header("Authorization") authorization: String, @Header("x-credential") xCredential: String, @Path("viewerId") viewerId: String, @Body heartbeatBody: HeartbeatBody): Call<Void>
}

class CreateViewerResult(val id: String, val offer: String, val signed_payload: String)

class StreamInitBody(val stage_id: String, val offer: String)
class StreamInitResult(val id: String, val answer: String, val signed_payload: String)

class IceCandidatesBody(val ice_candidates: Array<IndividualIceCandidate>, val signed_payload: String)
class AnswerBody(val answer: String, val signed_payload: String)

class IndividualIceCandidate(val candidate: String, val sdpMid: String, val sdpMLineIndex: Int)

class HeartbeatBody(val signed_payload: String)
