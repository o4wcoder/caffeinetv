package tv.caffeine.app.stage

/**
 * More than the endorsementCount any reaction will ever get, so a reaction can be assured
 * to compare as greater than another.
 */
private const val MORE_THAN_THE_MAX_ENDORSEMENT_COUNT_REALISTICALLY_POSSIBLE = 1_000_000_000.0

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
     */
    fun calculateReactionComparisonScore(
        age: Long = 0,
        endorsementCount: Int = 0,
        fromFollowedUser: Boolean = false,
        fromSelf: Boolean = false
    ): Double {
        val reactionScore = calculateReactionScore(age, endorsementCount)
        val isStale = reactionScore < 0
        val followedUserAdjustment = when {
            fromFollowedUser && !isStale -> MORE_THAN_THE_MAX_ENDORSEMENT_COUNT_REALISTICALLY_POSSIBLE
            else -> 0.0
        }
        val selfAdjustment = when {
            fromSelf && !isStale -> MORE_THAN_THE_MAX_ENDORSEMENT_COUNT_REALISTICALLY_POSSIBLE * 7
            else -> 0.0
        }
        return reactionScore + followedUserAdjustment + selfAdjustment
    }
}
