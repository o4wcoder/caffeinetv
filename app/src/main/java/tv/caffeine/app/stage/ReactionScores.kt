package tv.caffeine.app.stage

/**
 * More than the endorsementCount any reaction will ever get, so a reaction can be assured
 * to compare as greater than another.
 */
private const val MORE_THAN_THE_MAX_ENDORSEMENT_COUNT_REALISTICALLY_POSSIBLE = 100_000.0

/**
 * Buckets that allow priotization of reactions based on specific factors
 */
private const val EXTRA_SCORE_FOR_REACTION_FROM_ONESELF =
    MORE_THAN_THE_MAX_ENDORSEMENT_COUNT_REALISTICALLY_POSSIBLE * 1_000
private const val EXTRA_SCORE_FOR_REACTION_WITH_DIGITAL_ITEM =
    MORE_THAN_THE_MAX_ENDORSEMENT_COUNT_REALISTICALLY_POSSIBLE * 100
private const val EXTRA_SCORE_FOR_REACTION_WITH_MENTION =
    MORE_THAN_THE_MAX_ENDORSEMENT_COUNT_REALISTICALLY_POSSIBLE * 10
private const val EXTRA_SCORE_FOR_REACTION_FROM_FOLLOWED_USER =
    MORE_THAN_THE_MAX_ENDORSEMENT_COUNT_REALISTICALLY_POSSIBLE * 1

/**
 * Based on Tracer's reaction-scores.js
 */
class ReactionScores {

    /**
     * Calculates an absolute score for the reaction, where any score above 0 means it deserves
     * to still be seen by the user. This score decays over time but can be bolstered (with
     * progressively decreasing efficiency) by endorsements.
     */
    fun calculateReactionScore(age: Long = 0, endorsementCount: Int = 0): Double {
        return -1 * Math.pow(1.03, age.toDouble() / 1000.0) + (20 + endorsementCount) / 15.0
    }

    /**
     * Calculates a score similar to the basic reaction score, but for comparing reactions to each
     * other while factoring in extra information like whether someone the user follows authored it
     * or the user themself made it.
     * @param age: How old this reaction is, in milliseconds since the epoch
     * @param endorsementCount: How many endorsements (upvotes) this reaction has
     * @param fromSelf: True if this reaction was sent by the current user
     * @param hasDigitalItem: If this reaction has a digital item attached
     * @param hasMention: If this reaction has an @mention directed at the current user
     * @param fromFollowedUser: True if this reaction was sent by someone the current user follows
     */
    fun calculateReactionComparisonScore(
        age: Long = 0,
        endorsementCount: Int = 0,
        fromSelf: Boolean = false,
        hasDigitalItem: Boolean = false,
        hasMention: Boolean = false,
        fromFollowedUser: Boolean = false
    ): Double {
        val reactionScore = calculateReactionScore(age, endorsementCount)
        val isStale = reactionScore < 0

        val selfAdjustment = when {
            fromSelf && !isStale -> EXTRA_SCORE_FOR_REACTION_FROM_ONESELF
            else -> 0.0
        }
        val digitalItemAdjustment = when {
            hasDigitalItem && !isStale -> EXTRA_SCORE_FOR_REACTION_WITH_DIGITAL_ITEM
            else -> 0.0
        }
        val mentionAdjustment = when {
            hasMention && !isStale -> EXTRA_SCORE_FOR_REACTION_WITH_MENTION
            else -> 0.0
        }
        val followedUserAdjustment = when {
            fromFollowedUser && !isStale -> EXTRA_SCORE_FOR_REACTION_FROM_FOLLOWED_USER
            else -> 0.0
        }
        return reactionScore + selfAdjustment + digitalItemAdjustment + mentionAdjustment + followedUserAdjustment
    }
}