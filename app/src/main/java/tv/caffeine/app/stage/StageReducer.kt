package tv.caffeine.app.stage

import tv.caffeine.app.api.model.Message
import tv.caffeine.app.api.model.MessageWrapper

const val SMALL_STAGE_CUTOFF_SIZE = 5
const val SMALL_STAGE_MAX_SHARES = 2
const val LARGE_STAGE_MAX_SHARES = 3
const val REACTION_REPLACEMENT_IMMUNITY_TIME = 4 * 1000
const val MAXIMUM_NON_UPDATED_REACTION_AGE = 30 * 1000L

class StageReducer {
    private val reactionScores = ReactionScores() // TODO inject

    /**
     * Find a position for the reaction to be displayed in or another reaction's position to replace
     */
    fun determineReactionPosition(
        reactions: List<MessageWrapper>,
        incomingReaction: MessageWrapper,
        preferredPositions: List<Int>,
        maxVisibleReactions: Int,
        currentTime: Long
    ): Int {
        val existingReactionPosition = reactions.find { it.message.id == incomingReaction.message.id }?.position
        val visibleReactions = reactions.take(maxVisibleReactions)
        val maximumShareCount = if (maxVisibleReactions <= SMALL_STAGE_CUTOFF_SIZE) SMALL_STAGE_MAX_SHARES else LARGE_STAGE_MAX_SHARES
        val currentShareCount = visibleReactions.count { it.message.type == Message.Type.share }
        val replaceShare = incomingReaction.message.type == Message.Type.share && currentShareCount >= maximumShareCount
        val visibleReactionsPositions = visibleReactions.map { it.position }
        return when {
            existingReactionPosition != null -> existingReactionPosition
            visibleReactions.size < maxVisibleReactions && !replaceShare -> preferredPositions.find { it < maxVisibleReactions && !visibleReactionsPositions.contains(it) } ?: -1 // TODO should probably fall through or fail expectation
            else -> determineReactionReplacementPosition(visibleReactions, incomingReaction, currentTime, replaceShare)
        }
    }

    private fun determineReactionReplacementPosition(
        visibleReactions: List<MessageWrapper>,
        incomingReaction: MessageWrapper,
        currentTime: Long,
        replaceShare: Boolean
    ): Int {
        val reactionsOldEnoughForReplacement = visibleReactions.filterNot { currentTime - it.creationTime < REACTION_REPLACEMENT_IMMUNITY_TIME }
        val replaceableReactions = if (replaceShare) reactionsOldEnoughForReplacement.filter { it.message.type == Message.Type.share } else reactionsOldEnoughForReplacement
        return when {
            replaceableReactions.isEmpty() -> -1
            else -> {
                val incomingReactionScore = determineComparisonScore(incomingReaction, currentTime)
                val mostReplaceable = replaceableReactions.minBy { determineComparisonScore(it, currentTime) } ?: return -1
                val mostReplaceableScore = determineComparisonScore(mostReplaceable, currentTime)
                if (incomingReactionScore > mostReplaceableScore) mostReplaceable.position else -1
            }
        }
    }

    private fun determineComparisonScore(incomingReaction: MessageWrapper, currentTime: Long): Double {
        return reactionScores.calculateReactionComparisonScore(
            currentTime - incomingReaction.creationTime,
            incomingReaction.message.endorsementCount,
            incomingReaction.isFromSelf,
            incomingReaction.message.body.digitalItem != null,
            incomingReaction.hasMentions,
            incomingReaction.isFromFollowedUser)
    }

    fun handleProcessOldReactions(
        reactions: List<MessageWrapper>,
        currentTime: Long
    ): List<MessageWrapper> = reactions
            .map {
                val reactionScore = reactionScores.calculateReactionScore(currentTime - it.creationTime, it.message.endorsementCount)
                val isNowStale = reactionScore <= 0
                val becameStale = !it.isStale && isNowStale
                it.copy(isStale = isNowStale, lastUpdateTime = if (becameStale) currentTime else it.lastUpdateTime)
            }
            .filter {
                currentTime - it.lastUpdateTime <= MAXIMUM_NON_UPDATED_REACTION_AGE
            }
}
