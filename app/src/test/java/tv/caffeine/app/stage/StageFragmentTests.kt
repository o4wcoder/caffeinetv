package tv.caffeine.app.stage

import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.databinding.DataBindingComponent
import com.google.android.material.appbar.AppBarLayout
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.databinding.FragmentStageBinding
import tv.caffeine.app.profile.UserProfile

@RunWith(RobolectricTestRunner::class)
class StageFragmentVisibilityTests {
    @Rule @JvmField val instantExecutorRule = InstantTaskExecutorRule()

    lateinit var subject: StageFragment
    @MockK(relaxed = true) lateinit var gameLogoImageView: ImageView
    @MockK(relaxed = true) lateinit var avatarImageView: ImageView
    @MockK(relaxed = true) lateinit var backToLobbyButton: Button
    @MockK(relaxed = true) lateinit var followButton: Button
    @MockK(relaxed = true) lateinit var largeAvatarImageView: ImageView
    @MockK(relaxed = true) lateinit var liveIndicatorAndAvatarContainer: ConstraintLayout
    @MockK(relaxed = true) lateinit var liveIndicatorTextView: TextView
    @MockK(relaxed = true) lateinit var saySomethingTextView: TextView
    @MockK(relaxed = true) lateinit var showIsOverTextView: TextView
    @MockK(relaxed = true) lateinit var stageAppBar: AppBarLayout
    @MockK(relaxed = true) lateinit var avatarUsernameContainer: ConstraintLayout

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        subject = StageFragment(mockk(), mockk(), mockk(), mockk(), mockk())
        subject.binding = StageFragmentVisibilityTestBindings(
                avatarImageView,
                backToLobbyButton,
                followButton,
                gameLogoImageView,
                largeAvatarImageView,
                liveIndicatorAndAvatarContainer,
                liveIndicatorTextView,
                saySomethingTextView,
                showIsOverTextView,
                stageAppBar,
                avatarUsernameContainer
        )
    }

    @Test
    fun `hiding overlays hides app bar`() {
        subject.binding.stageAppbar.isVisible = true
        subject.hideOverlays()
        verify { subject.binding.stageAppbar.isVisible = false }
    }

    @Test
    fun `showing overlays shows app bar if it's included`() {
        subject.binding.stageAppbar.isVisible = false
        subject.showOverlays(true)
        verify { subject.binding.stageAppbar.isVisible = true }
    }

    @Test
    fun `showing overlays does not show the app bar if it's not included`() {
        subject.binding.stageAppbar.isVisible = false
        subject.showOverlays(false)
        verify { subject.binding.stageAppbar.isVisible = false }
    }

    @Test
    fun `showing overlays on an offline stage does not show live indicator`() {
        subject.stageIsLive = false
        subject.showOverlays()
        verify { subject.binding.liveIndicatorTextView.isVisible = false }
    }

    @Test
    fun `showing overlays on a live stage shows live indicator`() {
        subject.stageIsLive = true
        subject.showOverlays()
        verify { subject.binding.liveIndicatorTextView.isVisible = true }
    }

    @Test
    fun `showing overlays on an offline stage does not show game logo`() {
        subject.stageIsLive = false
        subject.showOverlays()
        verify { subject.binding.gameLogoImageView.isVisible = false }
    }

    @Test
    fun `showing overlays on a live stage shows game logo`() {
        subject.stageIsLive = true
        subject.showOverlays()
        verify { subject.binding.gameLogoImageView.isVisible = true }
    }

    @Test
    fun `hiding overlays on an offline stage hides game logo`() {
        subject.stageIsLive = true
        subject.hideOverlays()
        verify { subject.binding.gameLogoImageView.isVisible = false }
    }

    @Test
    fun `showing overlays does not change the visibility of the follow button`() {
        subject.showOverlays()
        verify(exactly = 0) { subject.binding.followButton.visibility = any() }
    }

    @Test
    fun `hiding overlays does not change the visibility of the follow button`() {
        subject.hideOverlays()
        verify(exactly = 0) { subject.binding.followButton.visibility = any() }
    }

    @Test
    fun `stage going offline shows offline views`() {
        subject.updateBroadcastOnlineState(false)
        verify { subject.binding.largeAvatarImageView.isVisible = true }
        verify { subject.binding.showIsOverTextView.isVisible = true }
        verify { subject.binding.backToLobbyButton.isVisible = true }
    }

    @Test
    fun `stage going live hides offline views`() {
        subject.updateBroadcastOnlineState(true)
        verify { subject.binding.largeAvatarImageView.isVisible = false }
        verify { subject.binding.showIsOverTextView.isVisible = false }
        verify { subject.binding.backToLobbyButton.isVisible = false }
    }

    @Test
    fun `stage going offline shows overlay but hides live indicator, game logo, and avatar+username container`() {
        subject.updateBroadcastOnlineState(false)
        verify { subject.binding.liveIndicatorAndAvatarContainer.isVisible = true }
        verify { subject.binding.gameLogoImageView.isVisible = false }
        verify { subject.binding.liveIndicatorTextView.isVisible = false }
        verify { subject.binding.avatarUsernameContainer.isVisible = false }
    }

    @Test
    fun `stage going live does not change visibility of game logo and small user avatar`() {
        subject.updateBroadcastOnlineState(true)
        verify(exactly = 0) { subject.binding.liveIndicatorAndAvatarContainer.visibility = any() }
        verify(exactly = 0) { subject.binding.gameLogoImageView.visibility = any() }
    }
}

private class StageFragmentVisibilityTestBindings(
    avatarImageView: ImageView,
    backToLobbyButton: Button,
    followButton: Button,
    gameLogoImageView: ImageView,
    largeAvatarImageView: ImageView,
    liveIndicatorAndAvatarContainer: ConstraintLayout,
    liveIndicatorTextView: TextView,
    saySomethingTextView: TextView,
    showIsOverTextView: TextView,
    stageAppBar: AppBarLayout,
    avatarUsernameContainer: ConstraintLayout
) : FragmentStageBinding(
        mockk<DataBindingComponent>(),
        mockk(),
        0,
        avatarImageView,
        avatarUsernameContainer,
        backToLobbyButton,
        mockk(),
        mockk(),
        mockk(),
        followButton,
        mockk(),
        gameLogoImageView,
        mockk(),
        largeAvatarImageView,
        liveIndicatorAndAvatarContainer,
        liveIndicatorTextView,
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        saySomethingTextView,
        mockk(),
        mockk(),
        mockk(),
        showIsOverTextView,
        stageAppBar,
        mockk(),
        mockk()
) {
    override fun setVariable(variableId: Int, value: Any?): Boolean {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun executeBindings() {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun onFieldChange(localFieldId: Int, `object`: Any?, fieldId: Int): Boolean {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun setUserProfile(userProfile: UserProfile?) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun invalidateAll() {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun hasPendingBindings(): Boolean {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
