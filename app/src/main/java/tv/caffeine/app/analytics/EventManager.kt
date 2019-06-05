package tv.caffeine.app.analytics

import com.google.gson.Gson
import timber.log.Timber
import tv.caffeine.app.api.EventBody
import tv.caffeine.app.api.EventsService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventManager @Inject constructor(
    private val eventsService: EventsService,
    private val gson: Gson
) {
    suspend fun sendEvent(eventBody: EventBody) {
        val result = eventsService.sendEvent(eventBody).awaitAndParseErrors(gson)
        when (result) {
            is CaffeineResult.Success -> Timber.d("Sending ${eventBody.eventType} event - Success")
            is CaffeineResult.Error -> Timber.e("Sending ${eventBody.eventType} event - Error")
            is CaffeineResult.Failure -> Timber.e(result.throwable)
        }
    }
}
