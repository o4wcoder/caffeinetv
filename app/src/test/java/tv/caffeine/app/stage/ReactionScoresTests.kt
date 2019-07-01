package tv.caffeine.app.stage

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.lessThan
import org.junit.Test

private const val ANCIENT_REACTION_AGE = 10 * 60 * 1000L
private const val INCREDIBLY_HUGE_ENDORSEMENT_COUNT = 100_000

/**
 * Based on Tracer's reaction-scores.test.js
 */
class ReactionScoresTests {

    class CalculateReactionScoreTests {
        private val subject = ReactionScores()

        @Test
        fun `gives a positive score to a reaction that has just arrived`() {
            val a = subject.calculateReactionScore(0, 0)

            assertThat(a, greaterThan(0.0))
        }

        @Test
        fun `gives a negative score to a very old reaction`() {
            val a = subject.calculateReactionScore(ANCIENT_REACTION_AGE, 0)

            assertThat(a, lessThan(0.0))
        }

        @Test
        fun `scores a newer (but otherwise identical) reaction higher`() {
            val a = subject.calculateReactionComparisonScore(2, 5)
            val b = subject.calculateReactionComparisonScore(10, 5)

            assertThat(a, greaterThan(b))
        }
    }

    class CalculateReactionComparisonScore {
        private val subject = ReactionScores()

        @Test
        fun `scores a reaction with more endorsements (but otherwise identical) higher`() {
            val a = subject.calculateReactionComparisonScore(10, 5)
            val b = subject.calculateReactionComparisonScore(10, 0)

            assertThat(a, greaterThan(b))
        }

        @Test
        fun `scores a much newer reaction with a little fewer endorsements higher`() {
            val a = subject.calculateReactionComparisonScore(10, 5)
            val b = subject.calculateReactionComparisonScore(10 * 1000, 10)

            assertThat(a, greaterThan(b))
        }

        @Test
        fun `scores a reaction from a followed user higher than a highly endorsed one`() {
            val a = subject.calculateReactionComparisonScore(0, 0, fromFollowedUser = true)
            val b = subject.calculateReactionComparisonScore(0, INCREDIBLY_HUGE_ENDORSEMENT_COUNT)

            assertThat(a, greaterThan(b))
        }

        @Test
        fun `scores a followed user's reaction above a less endorsed one from a followed user`() {
            val a = subject.calculateReactionComparisonScore(0, 25, fromFollowedUser = true)
            val b = subject.calculateReactionComparisonScore(0, 20, fromFollowedUser = true)

            assertThat(a, greaterThan(b))
        }

        @Test
        fun `scores a reaction from a followed user above a much older one from a followed user`() {
            val a = subject.calculateReactionComparisonScore(0, 25, fromFollowedUser = true)
            val b = subject.calculateReactionComparisonScore(ANCIENT_REACTION_AGE, 200, fromFollowedUser = true)

            assertThat(a, greaterThan(b))
        }

        @Test
        fun `scores a reaction from the current user higher than a super popular reaction`() {
            val a = subject.calculateReactionComparisonScore(0, 0, fromSelf = true)
            val b = subject.calculateReactionComparisonScore(0, INCREDIBLY_HUGE_ENDORSEMENT_COUNT)

            assertThat(a, greaterThan(b))
        }

        @Test
        fun `scores a reaction from the current user higher than one from someone they follow`() {
            val a = subject.calculateReactionComparisonScore(0, 0, fromSelf = true)
            val b = subject.calculateReactionComparisonScore(0, INCREDIBLY_HUGE_ENDORSEMENT_COUNT, fromFollowedUser = true)

            assertThat(a, greaterThan(b))
        }

        @Test
        fun `scores a reaction from the current user higher than less-endorsed one they also made`() {
            val a = subject.calculateReactionComparisonScore(0, 10, fromSelf = true)
            val b = subject.calculateReactionComparisonScore(0, 2, true)

            assertThat(a, greaterThan(b))
        }

        @Test
        fun `scores a reaction with a digital item above one with a mention from a friend`() {
            val a = subject.calculateReactionComparisonScore(0, 10, hasDigitalItem = true)
            val b = subject.calculateReactionComparisonScore(0, INCREDIBLY_HUGE_ENDORSEMENT_COUNT, hasMention = true, fromFollowedUser = true)

            assertThat(a, greaterThan(b))
        }

        @Test
        fun `scores a reaction with a digital item attached as lower than one's own message`() {
            val a = subject.calculateReactionComparisonScore(0, 0, hasDigitalItem = true)
            val b = subject.calculateReactionComparisonScore(0, 0, true)

            assertThat(a, lessThan(b))
        }

        @Test
        fun `scores a reaction with a mention from a friend higher than one from that friend`() {
            val a = subject.calculateReactionComparisonScore(0, 10, fromFollowedUser = true, hasMention = true)
            val b = subject.calculateReactionComparisonScore(0, INCREDIBLY_HUGE_ENDORSEMENT_COUNT, fromFollowedUser = true)

            assertThat(a, greaterThan(b))
        }

        @Test
        fun `scores a reaction with a mention higher than one without`() {
            val a = subject.calculateReactionComparisonScore(0, 0, hasMention = true)
            val b = subject.calculateReactionComparisonScore(0, INCREDIBLY_HUGE_ENDORSEMENT_COUNT)

            assertThat(a, greaterThan(b))
        }

        @Test
        fun `scores a reaction with a digital item attached as higher than any others`() {
            val a = subject.calculateReactionComparisonScore(0, 10, hasDigitalItem = true)
            val b = subject.calculateReactionComparisonScore(0, 2, hasMention = true, fromFollowedUser = true)

            assertThat(a, greaterThan(b))
        }

        // Since the scoring calculation yields very large numbers, make sure they aren't so large that
        // they'll cause weird number bugs
        @Test
        fun `a calculated score won't exceed the max safe int value except the user's own`() {
            val a = subject.calculateReactionComparisonScore(0, 1_000_000_000, hasDigitalItem = true, fromSelf = true, hasMention = true, fromFollowedUser = true)
            val b = Int.MAX_VALUE.toDouble()

            assertThat(a, lessThan(b))
        }
    }
}