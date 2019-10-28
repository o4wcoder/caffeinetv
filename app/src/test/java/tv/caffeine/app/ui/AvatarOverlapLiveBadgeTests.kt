package tv.caffeine.app.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.runner.AndroidJUnit4
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tv.caffeine.app.CaffeineApplication
import tv.caffeine.app.lobby.release.OnlineBroadcaster

@RunWith(AndroidJUnit4::class)
class AvatarOverlapLiveBadgeTests {

    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

    @MockK(relaxed = true) lateinit var fakeOnlineBroadcaster: OnlineBroadcaster

    private val context = ApplicationProvider.getApplicationContext<CaffeineApplication>()
    private val subject = AvatarOverlapLiveBadge(context)

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `test lobby binding adapter`() {
        subject.setOnlineBroadcaster(fakeOnlineBroadcaster)
        verify(exactly = 1) { subject.lobbyBroadcaster = fakeOnlineBroadcaster }
    }
}