package tv.caffeine.app.navigation

import androidx.core.os.bundleOf
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.lobby.LobbySwipeFragmentDirections
import tv.caffeine.app.stage.StagePagerFragmentArgs
import tv.caffeine.app.util.broadcasterUsername

@RunWith(RobolectricTestRunner::class)
class StageFragmentArgsTests {

    @Test
    fun broadcasterUsernameIsParsedCorrectly() {
        val action = LobbySwipeFragmentDirections.actionLobbySwipeFragmentToStagePagerFragment("username")
        val subject = StagePagerFragmentArgs.fromBundle(action.arguments)
        Assert.assertEquals("username", subject.broadcasterUsername())
    }

    @Test
    fun broadcasterUsernameByItselfIsParsedCorrectly() {
        val subject = StagePagerFragmentArgs.fromBundle(bundleOf("broadcastLink" to "username"))
        Assert.assertEquals("username", subject.broadcasterUsername())
    }

    @Test
    fun broadcasterUsernameWithParametersIsParsedCorrectly() {
        val subject = StagePagerFragmentArgs.fromBundle(bundleOf("broadcastLink" to "username?bst=blah"))
        Assert.assertEquals("username", subject.broadcasterUsername())
    }
}
