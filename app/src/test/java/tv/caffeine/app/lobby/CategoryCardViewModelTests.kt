package tv.caffeine.app.lobby

import android.content.Context
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.R
import tv.caffeine.app.lobby.fragment.ClusterData
import tv.caffeine.app.lobby.release.CategoryCardViewModel
import tv.caffeine.app.lobby.release.NavigationCommand
import tv.caffeine.app.net.ServerConfig
import tv.caffeine.app.settings.InMemorySettingsStorage
import tv.caffeine.app.test.observeForTesting

@RunWith(RobolectricTestRunner::class)
class CategoryCardViewModelTests {

    lateinit var context: Context
    private val serverConfig = ServerConfig(InMemorySettingsStorage())

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun `show the name and gradient if the overlay image is null`() {
        val card = buildCategoryCard("name", "/background.jpg", null)
        val viewModel = CategoryCardViewModel(card, context, serverConfig)
        assertEquals("name", viewModel.name)
        assertEquals(View.VISIBLE, viewModel.nameVisibility)
        assertNotNull(viewModel.gradientDrawable)
    }

    @Test
    fun `do not show the name or gradient if the overlay image is not null`() {
        val card = buildCategoryCard("name", "/background.jpg", "/overlay.jpg")
        val viewModel = CategoryCardViewModel(card, context, serverConfig)
        assertEquals("name", viewModel.name) // the hidden name is still used as the content description
        assertEquals(View.GONE, viewModel.nameVisibility)
        assertNull(viewModel.gradientDrawable)
    }

    @Test
    fun `the image url is a full url if the image path is not null`() {
        val card = buildCategoryCard("name", "/background.jpg", "/overlay.jpg")
        val viewModel = CategoryCardViewModel(card, context, serverConfig)
        assertEquals("${serverConfig.images}/background.jpg", viewModel.backgroundImageUrl)
        assertEquals("${serverConfig.images}/overlay.jpg", viewModel.overlayImageUrl)
    }

    @Test
    fun `the image url is null if the image path is null`() {
        val card = buildCategoryCard("name", null, null)
        val viewModel = CategoryCardViewModel(card, context, serverConfig)
        assertNull(viewModel.backgroundImageUrl)
        assertNull(viewModel.overlayImageUrl)
    }

    @Test
    fun `clicking the category card navigates to the detail page`() {
        val card = buildCategoryCard("name", null, null)
        val viewModel = CategoryCardViewModel(card, context, serverConfig)
        viewModel.cardClicked()
        viewModel.navigationCommands.observeForTesting {
            val navigationCommand = it.peekContent()
            Assert.assertTrue(navigationCommand is NavigationCommand.To)
            val directions = (navigationCommand as NavigationCommand.To).directions
            assertEquals(R.id.action_global_lobbyDetailFragment, directions.actionId)
            val cardId = navigationCommand.directions.arguments.getString("cardId")
            assertEquals("cardId_name", cardId)
        }
    }

    private fun buildCategoryCard(name: String, backgroundImagePath: String?, overlayImagePath: String?) = ClusterData.CategoryCard(
        "",
        "cardId_$name",
        0,
        name,
        backgroundImagePath,
        overlayImagePath
    )
}