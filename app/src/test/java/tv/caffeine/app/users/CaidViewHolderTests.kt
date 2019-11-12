package tv.caffeine.app.users

import android.content.Context
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.Navigation
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import org.junit.Before
import org.junit.Test
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.databinding.CaidItemBinding
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.ThemeColor
import tv.caffeine.app.util.safeNavigate

class CaidViewHolderTests {

    private lateinit var classicCaidViewHolder: ClassicCaidViewHolder
    private lateinit var releaseCaidViewHolder: ReleaseCaidViewHolder
    @MockK private lateinit var itemView: View
    @MockK private lateinit var followHandler: FollowManager.FollowHandler
    @MockK private lateinit var scope: CoroutineScope
    @MockK private lateinit var binding: CaidItemBinding
    @MockK private lateinit var context: Context
    @MockK private lateinit var navController: NavController
    @MockK private lateinit var userNavigationCallback: UserNavigationCallback

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkStatic(Navigation::class)
        every { Navigation.findNavController(any()) } returns navController
        every { binding.root } returns itemView
        every { binding.followStarViewModel = any() } just Runs
        every { binding.liveStatusIndicatorViewModel = any() } just Runs
        every { itemView.context } returns context
        every { userNavigationCallback.onUserNavigation(any()) } just Runs

        classicCaidViewHolder = ClassicCaidViewHolder(itemView, followHandler, scope)
        releaseCaidViewHolder = ReleaseCaidViewHolder(binding, scope, ThemeColor.DARK) { _, _ -> }
    }

    @Test
    fun `the user navigation callback is called instead of the nav controller from the view if the callback is not null in the classic view holder`() {
        val action = MainNavDirections.actionGlobalProfileFragment("caid")
        every { navController.navigate(action) } just Runs
        classicCaidViewHolder.performUserNavigation(action, userNavigationCallback, itemView)
        verify(exactly = 1) { userNavigationCallback.onUserNavigation(action) }
        verify(exactly = 0) { navController.safeNavigate(action) }
    }

    @Test
    fun `the user navigation callback is called instead of the nav controller from the view if the callback is not null in the release view holder`() {
        val action = MainNavDirections.actionGlobalStagePagerFragment("username")
        every { navController.navigate(action) } just Runs
        releaseCaidViewHolder.performUserNavigation(action, userNavigationCallback, itemView)
        verify(exactly = 1) { userNavigationCallback.onUserNavigation(action) }
        verify(exactly = 0) { navController.safeNavigate(action) }
    }

    @Test
    fun `the nav controller from the view will be called instead of the callback if the callback is null in the classic view holder`() {
        val action = MainNavDirections.actionGlobalProfileFragment("caid")
        every { navController.navigate(action) } just Runs
        classicCaidViewHolder.performUserNavigation(action, null, itemView)
        verify(exactly = 0) { userNavigationCallback.onUserNavigation(action) }
        verify(exactly = 1) { navController.safeNavigate(action) }
    }

    @Test
    fun `the nav controller from the view will be called instead of the callback if the callback is null in the release view holder`() {
        val action = MainNavDirections.actionGlobalStagePagerFragment("username")
        every { navController.navigate(action) } just Runs
        releaseCaidViewHolder.performUserNavigation(action, null, itemView)
        verify(exactly = 0) { userNavigationCallback.onUserNavigation(action) }
        verify(exactly = 1) { navController.safeNavigate(action) }
    }
}