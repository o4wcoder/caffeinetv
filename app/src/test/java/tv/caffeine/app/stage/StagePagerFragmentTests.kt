package tv.caffeine.app.stage

import io.mockk.MockKAnnotations
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StagePagerFragmentTests {

    lateinit var fragment: StagePagerFragment

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        fragment = StagePagerFragment(mockk(), mockk(), mockk(), mockk())
    }

    @Test
    fun `the index of the stage among swipeable stages is the same index in the lobby if it exists there`() {
        val initialBroadcaster = "b"
        val lobbyBroadcasters = listOf("a", "b", "c")
        val (broadcasters, index) = fragment.configureBroadcasters(initialBroadcaster, lobbyBroadcasters)
        assertEquals(listOf("a", "b", "c"), broadcasters)
        assertEquals(1, index)
    }

    @Test
    fun `the index of the stage among swipeable stages is 0 if it does not exist in the lobby`() {
        val initialBroadcaster = "d"
        val lobbyBroadcasters = listOf("a", "b", "c")
        val (broadcasters, index) = fragment.configureBroadcasters(initialBroadcaster, lobbyBroadcasters)
        assertEquals(listOf("d", "a", "b", "c"), broadcasters)
        assertEquals(0, index)
    }
}
