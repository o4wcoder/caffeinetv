package tv.caffeine.app.lobby

import com.google.gson.Gson
import timber.log.Timber
import tv.caffeine.app.api.Counters
import tv.caffeine.app.api.EventsService
import tv.caffeine.app.api.LobbyService
import tv.caffeine.app.api.StatsCounter
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.api.model.awaitAndParseErrors
import javax.inject.Inject

class LoadLobbyUseCase @Inject constructor(
        private val lobbyService: LobbyService,
        private val eventsService: EventsService,
        private val gson: Gson
) {
    suspend operator fun invoke(): CaffeineResult<Lobby> {
        val result = lobbyService.loadLobby().awaitAndParseErrors(gson)
        if (result is CaffeineResult.Success) sendLobbyCounters()
        return result
    }

    private suspend fun sendLobbyCounters() {
        val stats = Counters(listOf(StatsCounter("android.loadlobbywithsections.counter", 1)))
        val sendStatsResult = eventsService.sendCounters(stats).awaitAndParseErrors(gson)
        when (sendStatsResult) {
            is CaffeineResult.Success -> Timber.d("Successfully sent lobby stats")
            is CaffeineResult.Error -> Timber.e(Exception("Error sending lobby stats"))
            is CaffeineResult.Failure -> Timber.e(sendStatsResult.throwable)
        }
    }
}
