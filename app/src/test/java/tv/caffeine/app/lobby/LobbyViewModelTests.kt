package tv.caffeine.app.lobby

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tv.caffeine.app.api.VersionCheckError
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.feature.LoadFeatureConfigUseCase
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.update.IsVersionSupportedCheckUseCase
import tv.caffeine.app.util.CoroutinesTestRule

class LobbyViewModelTests {
    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()
    @get:Rule val coroutinesTestRule = CoroutinesTestRule()

    lateinit var subject: LobbyViewModel

    @MockK(relaxed = true) lateinit var followManager: FollowManager
    @MockK(relaxed = true) lateinit var loadLobbyUseCase: LoadLobbyUseCase
    @MockK(relaxed = true) lateinit var loadFeatureConfigUseCase: LoadFeatureConfigUseCase
    @MockK(relaxed = true) lateinit var isVersionSupportedUseCase: IsVersionSupportedCheckUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        subject = LobbyViewModel(followManager, loadLobbyUseCase, loadFeatureConfigUseCase, isVersionSupportedUseCase)
    }

    @Test
    fun `refreshing lobby repeatedly calls feature config only once`() {
        coEvery { isVersionSupportedUseCase() } returns CaffeineEmptyResult.Success
        subject.refresh()
        subject.refresh()
        coVerify(exactly = 1) { loadFeatureConfigUseCase() }
    }

    @Test
    fun `refreshing lobby repeatedly calls load lobby every time`() {
        coEvery { isVersionSupportedUseCase() } returns CaffeineEmptyResult.Success
        subject.refresh()
        subject.refresh()
        subject.refresh()
        coVerify(exactly = 3) { loadLobbyUseCase() }
    }

    @Test
    fun `unsupported version prevents lobby from loading`() {
        coEvery { isVersionSupportedUseCase() } returns CaffeineEmptyResult.Error(VersionCheckError())
        subject.refresh()
        coVerify(exactly = 0) { loadLobbyUseCase() }
    }
}
