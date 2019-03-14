package tv.caffeine.app.navigation

import androidx.core.os.bundleOf
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import tv.caffeine.app.lobby.LobbyFragmentDirections
import tv.caffeine.app.stage.StageFragmentArgs
import tv.caffeine.app.util.broadcasterUsername

@RunWith(AndroidJUnit4::class)
class StageFragmentArgsTests {

    @Test
    fun broadcasterUsernameIsParsedCorrectly() {
        val action = LobbyFragmentDirections.actionLobbyFragmentToStageFragment("username")
        val subject = StageFragmentArgs.fromBundle(action.arguments)
        Assert.assertEquals("username", subject.broadcasterUsername())
    }

    @Test
    fun broadcasterUsernameByItselfIsParsedCorrectly() {
        val subject = StageFragmentArgs.fromBundle(bundleOf("broadcastLink" to "username"))
        Assert.assertEquals("username", subject.broadcasterUsername())
    }

    @Test
    fun broadcasterUsernameWithParametersIsParsedCorrectly() {
        val subject = StageFragmentArgs.fromBundle(bundleOf("broadcastLink" to "username?bst=blah"))
        Assert.assertEquals("username", subject.broadcasterUsername())
    }

}