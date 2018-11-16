package tv.caffeine.app.api

import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import tv.caffeine.app.di.ApiConfig

interface EventsService {
    @POST(ApiConfig.EVENTS_SERVICE_EVENTS_PATH)
    fun sendEvent(@Body eventBody: EventBody): Deferred<Response<Any>>
}

class EventBody(val eventType: String, val eventSource: String = "android", val environment: String = "production", val data: Map<String, Any>)
