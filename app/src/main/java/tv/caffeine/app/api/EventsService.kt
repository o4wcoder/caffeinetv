package tv.caffeine.app.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import tv.caffeine.app.di.ApiConfig

interface EventsService {
    @POST(ApiConfig.EVENTS_SERVICE_EVENTS_PATH)
    fun sendEvent(@Body eventBody: EventBody): Call<Void>
}

class EventBody(val eventType: String, val eventSource: String = "android", val environment: String = "production", val data: Map<String, Any>)
