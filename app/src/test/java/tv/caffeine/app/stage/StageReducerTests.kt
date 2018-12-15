package tv.caffeine.app.stage

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.lessThan
import org.junit.Test
import tv.caffeine.app.api.model.Message
import tv.caffeine.app.api.model.MessageWrapper
import tv.caffeine.app.api.model.User


private val dummyMessageBody = Message.Body("")
private val dummyPublisher = User("0", "", null, null, "", 0, 0, false, null, "", mapOf(), null, null, "", null, null, null, false, false, null)
private val preferredPositions = listOf(0, 1, 2, 3, 4, 5, 6)

private const val CURRENT_BASELINE_TIME = 1526331991213L
private const val EXPIRED_TIME = CURRENT_BASELINE_TIME - MAXIMUM_NON_UPDATED_REACTION_AGE - 1
private const val STALE_TIME = CURRENT_BASELINE_TIME - MAXIMUM_NON_UPDATED_REACTION_AGE

private const val currentTime = CURRENT_BASELINE_TIME
//LocalDateTime.of(2018, Month.APRIL, 16, 9, 49, 18).toInstant(ZoneOffset.ofHours(-7)).toEpochMilli()

private val OLD_STATE_WITH_REACTIONS_ON_THE_STAGE = listOf(
        MessageWrapper(Message(dummyPublisher, "0", Message.Type.share, dummyMessageBody), creationTime = EXPIRED_TIME, lastUpdateTime = STALE_TIME, position = 0, isStale = false),
        MessageWrapper(Message(dummyPublisher, "1", Message.Type.reaction, dummyMessageBody), creationTime = EXPIRED_TIME, lastUpdateTime = STALE_TIME, position = 1, isStale = true),
        MessageWrapper(Message(dummyPublisher, "2", Message.Type.reaction, dummyMessageBody), creationTime = CURRENT_BASELINE_TIME, lastUpdateTime = CURRENT_BASELINE_TIME, position = 2, isStale = false),
        MessageWrapper(Message(dummyPublisher, "3", Message.Type.share, dummyMessageBody), creationTime = 0, lastUpdateTime = EXPIRED_TIME, position = 3, isStale = true)
)

private val EXPECTED_STATE_WITH_REACTIONS_MARKED_AS_STALE_AND_OLD = listOf(
        MessageWrapper(Message(dummyPublisher, "0", Message.Type.share, dummyMessageBody), creationTime = EXPIRED_TIME, lastUpdateTime = CURRENT_BASELINE_TIME, position = 0, isStale = true),
        MessageWrapper(Message(dummyPublisher, "1", Message.Type.reaction, dummyMessageBody), creationTime = EXPIRED_TIME, lastUpdateTime = STALE_TIME, position = 1, isStale = true),
        MessageWrapper(Message(dummyPublisher, "2", Message.Type.reaction, dummyMessageBody), creationTime = CURRENT_BASELINE_TIME, lastUpdateTime = CURRENT_BASELINE_TIME, position = 2, isStale = false)
)

/**
 * Based on Tracer's stage.reducer.test.js
 */
class StageReducerTests {
    class DetermineReactionPositionTests {

        private val subject = StageReducer()

        private val mockReaction = MessageWrapper(Message(dummyPublisher, "123", Message.Type.reaction, dummyMessageBody), currentTime, position = -1)
        private val mockShareReaction = MessageWrapper(Message(dummyPublisher, "456", Message.Type.share, dummyMessageBody), currentTime, position = -1)

        private val twoMockReactions = listOf(
                MessageWrapper(Message(dummyPublisher, "0", Message.Type.reaction, dummyMessageBody), currentTime, position = 0),
                MessageWrapper(Message(dummyPublisher, "1", Message.Type.reaction, dummyMessageBody), currentTime, position = 1)
        )
        private val twoMockShareReactions = listOf(
                MessageWrapper(Message(dummyPublisher, "0", Message.Type.share, dummyMessageBody), currentTime, position = 0),
                MessageWrapper(Message(dummyPublisher, "1", Message.Type.share, dummyMessageBody), currentTime, position = 1)
        )
        private val twoMockShareReactionsAtDifferentTimes = listOf(
                MessageWrapper(Message(dummyPublisher, "0", Message.Type.share, dummyMessageBody), currentTime, position = 0),
                MessageWrapper(Message(dummyPublisher, "1", Message.Type.share, dummyMessageBody), currentTime - 5000L, position = 1)
        )
        private val fourMockVariedReactions = listOf(
                MessageWrapper(Message(dummyPublisher, "0", Message.Type.share, dummyMessageBody), currentTime, position = 0),
                MessageWrapper(Message(dummyPublisher, "1", Message.Type.reaction, dummyMessageBody), currentTime - 5000L, position = 1),
                MessageWrapper(Message(dummyPublisher, "2", Message.Type.reaction, dummyMessageBody), currentTime, position = 2),
                MessageWrapper(Message(dummyPublisher, "3", Message.Type.reaction, dummyMessageBody), currentTime, position = 3)
        )

        @Test fun `places the first reaction in the first position`() {
            val position = subject.determineReactionPosition(listOf(), mockReaction, preferredPositions, SMALL_STAGE_CUTOFF_SIZE, currentTime)
            assertThat(position, equalTo(0))
        }

        @Test fun `ignores a reaction position that's now invalid`() {
            val position = subject.determineReactionPosition(twoMockReactions, mockReaction, listOf(5, 4, 3, 2), SMALL_STAGE_CUTOFF_SIZE, currentTime)
            assertThat(position, equalTo(4))
        }

        @Test fun `drops a new share reaction if there's no eligible position for it`() {
            val position = subject.determineReactionPosition(twoMockShareReactions, mockShareReaction, preferredPositions, SMALL_STAGE_CUTOFF_SIZE, currentTime)
            assertThat(position, lessThan(0))
        }

        @Test fun `swaps a new share reaction if there's an eligible position for it`() {
            val position = subject.determineReactionPosition(twoMockShareReactionsAtDifferentTimes, mockShareReaction, preferredPositions, SMALL_STAGE_CUTOFF_SIZE, currentTime)
            assertThat(position, equalTo(1))
        }

        @Test fun `adds a new share reaction if there's space for it`() {
            val position = subject.determineReactionPosition(twoMockShareReactionsAtDifferentTimes, mockShareReaction, preferredPositions, SMALL_STAGE_CUTOFF_SIZE + 1, currentTime)
            assertThat(position, equalTo(2))
        }

        @Test fun `replaces an old text reaction with a share`() {
            val position = subject.determineReactionPosition(fourMockVariedReactions, mockShareReaction, preferredPositions, SMALL_STAGE_CUTOFF_SIZE - 1, currentTime)
            assertThat(position, equalTo(1))
        }

    }

    class HandleProcessOldReactionsTests {
        private val subject = StageReducer()

        @Test fun `filters out old reactions`() {
            val initialState = OLD_STATE_WITH_REACTIONS_ON_THE_STAGE
            val expectedState = EXPECTED_STATE_WITH_REACTIONS_MARKED_AS_STALE_AND_OLD
            assertThat(subject.handleProcessOldReactions(initialState, currentTime), equalTo(expectedState))
        }

    }

}
