package tv.caffeine.app.analytics

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import org.threeten.bp.Clock
import tv.caffeine.app.api.LobbyCardClickedEvent
import tv.caffeine.app.api.LobbyClickedEventData
import tv.caffeine.app.api.LobbyFollowClickedEvent
import tv.caffeine.app.api.LobbyImpressionEvent
import tv.caffeine.app.api.LobbyImpressionEventData
import tv.caffeine.app.api.model.makeLobbyImpressionEventData
import tv.caffeine.app.ext.seconds
import tv.caffeine.app.lobby.SingleCard
import tv.caffeine.app.session.FollowManager

class LobbyImpressionAnalytics @AssistedInject constructor(
    @Assisted private val payloadId: String,
    private val followManager: FollowManager,
    private val eventManager: EventManager,
    private val clock: Clock
) {

    @AssistedInject.Factory
    interface Factory {
        fun create(payloadId: String): LobbyImpressionAnalytics
    }

    suspend fun followClicked(singleCard: SingleCard) {
        val eventData = makeLobbyClickedEventData(singleCard) ?: return
        eventManager.sendEvent(LobbyFollowClickedEvent(eventData))
    }

    suspend fun cardClicked(singleCard: SingleCard) {
        val eventData = makeLobbyClickedEventData(singleCard) ?: return
        eventManager.sendEvent(LobbyCardClickedEvent(eventData))
    }

    suspend fun sendImpressionEventData(singleCard: SingleCard) {
        val eventData = getLobbyImpressionEventData(singleCard)
        eventManager.sendEvent(LobbyImpressionEvent(eventData))
    }

    private fun makeLobbyClickedEventData(singleCard: SingleCard): LobbyClickedEventData? {
        val caid = followManager.currentUserDetails()?.caid
        val stageId = singleCard.broadcaster.user.stageId
        val timestamp = clock.seconds()
        return LobbyClickedEventData(payloadId, caid, stageId, timestamp)
    }

    private fun getLobbyImpressionEventData(singleCard: SingleCard): LobbyImpressionEventData =
        singleCard.broadcaster.makeLobbyImpressionEventData(payloadId, clock.seconds())
}
