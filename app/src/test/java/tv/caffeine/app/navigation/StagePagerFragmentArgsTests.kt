package tv.caffeine.app.navigation

import androidx.core.os.bundleOf
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.lobby.LobbySwipeFragmentDirections
import tv.caffeine.app.stage.StagePagerFragmentArgs
import tv.caffeine.app.util.broadcasterUsername

@RunWith(RobolectricTestRunner::class)
class StagePagerFragmentArgsTests {

    @Test
    fun `broadcaster username is parsed correctly`() {
        val action = LobbySwipeFragmentDirections.actionLobbySwipeFragmentToStagePagerFragment("username")
        val subject = StagePagerFragmentArgs.fromBundle(action.arguments)
        assertEquals("username", subject.broadcasterUsername())
    }

    @Test
    fun `broadcaster username by itself is parsed correctly`() {
        val subject = StagePagerFragmentArgs.fromBundle(bundleOf("broadcastLink" to "username"))
        assertEquals("username", subject.broadcasterUsername())
    }

    @Test
    fun `broadcaster username with parameters is parsed correctly`() {
        val subject = StagePagerFragmentArgs.fromBundle(bundleOf("broadcastLink" to "username?bst=blah"))
        assertEquals("username", subject.broadcasterUsername())
    }
}
