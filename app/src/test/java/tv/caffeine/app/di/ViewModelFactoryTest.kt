package tv.caffeine.app.di

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import tv.caffeine.app.CaffeineApplication
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.auth.LandingFragment
import tv.caffeine.app.auth.LegalAgreementViewModel
import tv.caffeine.app.auth.SignInViewModel
import tv.caffeine.app.broadcast.GuideViewModel
import tv.caffeine.app.broadcast.LiveHostableBroadcastersViewModel
import tv.caffeine.app.explore.ExploreViewModel
import tv.caffeine.app.lobby.FeaturedProgramGuideViewModel
import tv.caffeine.app.lobby.LobbyViewModel
import tv.caffeine.app.notifications.NotificationsViewModel
import tv.caffeine.app.profile.DeleteAccountViewModel
import tv.caffeine.app.profile.IgnoreUserViewModel
import tv.caffeine.app.profile.MyProfileViewModel
import tv.caffeine.app.profile.ProfileViewModel
import tv.caffeine.app.profile.ReportUserViewModel
import tv.caffeine.app.profile.UpdateProfileViewModel
import tv.caffeine.app.session.SessionCheckViewModel
import tv.caffeine.app.settings.GoldBundlesViewModel
import tv.caffeine.app.settings.NotificationSettingsViewModel
import tv.caffeine.app.settings.SettingsViewModel
import tv.caffeine.app.settings.TransactionHistoryViewModel
import tv.caffeine.app.stage.ChatViewModel
import tv.caffeine.app.stage.DICatalogViewModel
import tv.caffeine.app.stage.FriendsWatchingViewModel
import tv.caffeine.app.stage.SendDigitalItemViewModel
import tv.caffeine.app.users.FollowersViewModel
import tv.caffeine.app.users.FollowingViewModel
import tv.caffeine.app.users.IgnoredUsersViewModel
import tv.caffeine.app.wallet.WalletViewModel

@RunWith(ParameterizedRobolectricTestRunner::class)
class ViewModelFactoryTest(private val modelClass: Class<out ViewModel>) {
    @Rule @JvmField val instantExecutorRule = InstantTaskExecutorRule()
    lateinit var subject: ViewModelFactory

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "can create a view model {0}")
        fun data() = listOf(
                SignInViewModel::class.java,
                LegalAgreementViewModel::class.java,
                LobbyViewModel::class.java,
                SessionCheckViewModel::class.java,
                ExploreViewModel::class.java,
                NotificationsViewModel::class.java,
                DICatalogViewModel::class.java,
                WalletViewModel::class.java,
                ProfileViewModel::class.java,
                IgnoredUsersViewModel::class.java,
                IgnoreUserViewModel::class.java,
                ReportUserViewModel::class.java,
                FollowingViewModel::class.java,
                FollowersViewModel::class.java,
                SettingsViewModel::class.java,
                NotificationSettingsViewModel::class.java,
                DeleteAccountViewModel::class.java,
                MyProfileViewModel::class.java,
                TransactionHistoryViewModel::class.java,
                GoldBundlesViewModel::class.java,
                UpdateProfileViewModel::class.java,
                ChatViewModel::class.java,
                FriendsWatchingViewModel::class.java,
                SendDigitalItemViewModel::class.java,
                GuideViewModel::class.java,
                FeaturedProgramGuideViewModel::class.java,
                LiveHostableBroadcastersViewModel::class.java
        ).map { arrayOf(it) }
    }

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<CaffeineApplication>()
        val testComponent = DaggerTestComponent.builder().create(app)
        app.setApplicationInjector(testComponent)
        val directions = MainNavDirections.actionGlobalLandingFragment(null)
        val scenario = launchFragmentInContainer<LandingFragment>(directions.arguments)
        val navController = mockk<NavController>(relaxed = true)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
            subject = it.viewModelFactory
        }
    }

    @Test
    fun `can create a view model`() {
        assertNotNull(subject.create(modelClass))
    }

}
