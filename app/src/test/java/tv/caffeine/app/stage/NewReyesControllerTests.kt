package tv.caffeine.app.stage

import android.media.AudioManager
import io.mockk.MockKAnnotations
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.webrtc.AudioTrack
import java.util.concurrent.ConcurrentHashMap

@RunWith(RobolectricTestRunner::class)
class NewReyesControllerTests {
    lateinit var subject: NewReyesController

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `all audio tracks are enabled when the stage is not muted and it gains audio focus`() {
        subject = buildNewReyesController(false)
        subject.onAudioFocusChangeListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        verify(exactly = 1) { subject.audioTracks["0"]?.setEnabled(true) }
        verify(exactly = 1) { subject.audioTracks["1"]?.setEnabled(true) }
    }

    @Test
    fun `all audio tracks are disabled when the stage is muted and it gains audio focus`() {
        subject = buildNewReyesController(true)
        subject.onAudioFocusChangeListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        verify(exactly = 1) { subject.audioTracks["0"]?.setEnabled(false) }
        verify(exactly = 1) { subject.audioTracks["1"]?.setEnabled(false) }
    }

    @Test
    fun `all audio tracks are disabled when the stage loses audio focus`() {
        subject = buildNewReyesController(false)
        subject.onAudioFocusChangeListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        verify(exactly = 1) { subject.audioTracks["0"]?.setEnabled(false) }
        verify(exactly = 1) { subject.audioTracks["1"]?.setEnabled(false) }

        subject = buildNewReyesController(true)
        subject.onAudioFocusChangeListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        verify(exactly = 1) { subject.audioTracks["0"]?.setEnabled(false) }
        verify(exactly = 1) { subject.audioTracks["1"]?.setEnabled(false) }
    }

    private fun buildNewReyesController(shouldMuteAudio: Boolean): NewReyesController {
        val controller = NewReyesController(
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            "username",
            shouldMuteAudio)
        val audioTracks: MutableMap<String, AudioTrack> = ConcurrentHashMap()
        audioTracks["0"] = mockk(relaxed = true)
        audioTracks["1"] = mockk(relaxed = true)
        controller.audioTracks = audioTracks
        return controller
    }
}
