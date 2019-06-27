package tv.caffeine.app.api.model

import tv.caffeine.app.api.LobbyImpressionEventData

fun Lobby.Broadcaster.makeLobbyImpressionEventData(payloadId: String, renderedAt: Long) =
    LobbyImpressionEventData(
        payloadId,
        user.caid,
        user.stageId,
        user.isFeatured,
        broadcast != null,
        displayOrder,
        followingViewers.map { it.caid },
        clusterId,
        renderedAt
    )
