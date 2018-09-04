package tv.caffeine.app.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface EventsService {
    @POST("v1/events")
    fun sendEvent(@Body eventBody: EventBody): Call<Void>
}

class EventBody(val eventType: String, val eventSource: String = "android", val environment: String = "production", val data: Map<String, Any>)
