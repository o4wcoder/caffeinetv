package tv.caffeine.app.analytics

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import org.threeten.bp.Clock
import tv.caffeine.app.api.LobbyCardClickedEvent
import tv.caffeine.app.api.LobbyClickedEventData
import tv.caffeine.app.api.LobbyFollowClickedEvent
import tv.caffeine.app.api.LobbyImpressionEvent
import tv.caffeine.app.api.LobbyImpressionEventData
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.Lobby
import tv.caffeine.app.ext.seconds
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

    suspend fun followClicked(broadcaster: Lobby.Broadcaster) {
        val eventData = makeLobbyClickedEventData(broadcaster) ?: return
        eventManager.sendEvent(LobbyFollowClickedEvent(eventData))
    }

    suspend fun cardClicked(broadcaster: Lobby.Broadcaster) {
        val eventData = makeLobbyClickedEventData(broadcaster) ?: return
        eventManager.sendEvent(LobbyCardClickedEvent(eventData))
    }

    suspend fun sendImpressionEventData(broadcaster: Lobby.Broadcaster) {
        val caid = followManager.currentUserDetails()?.caid
        val eventData = broadcaster.makeLobbyImpressionEventData(payloadId, caid, clock.seconds())
        eventManager.sendEvent(LobbyImpressionEvent(eventData))
    }

    private fun makeLobbyClickedEventData(broadcaster: Lobby.Broadcaster): LobbyClickedEventData? {
        val caid = followManager.currentUserDetails()?.caid
        val stageId = broadcaster.user.stageId
        val timestamp = clock.seconds()
        return LobbyClickedEventData(payloadId, caid, stageId, timestamp)
    }
}

fun Lobby.Broadcaster.makeLobbyImpressionEventData(payloadId: String, caid: CAID?, renderedAt: Long) =
    LobbyImpressionEventData(
        payloadId,
        caid,
        user.stageId,
        user.isFeatured,
        broadcast != null,
        displayOrder,
        followingViewers?.map { it.caid } ?: emptyList(),
        clusterId,
        broadcast?.contentId,
        renderedAt
    )
