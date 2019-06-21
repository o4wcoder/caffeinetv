package tv.caffeine.app.settings

import android.content.SharedPreferences
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.caffeine.app.feature.Feature
import tv.caffeine.app.feature.FeatureConfig

class AutoPlayConfigTests {
    @MockK lateinit var featureConfig: FeatureConfig
    @MockK lateinit var sharedPrefs: SharedPreferences

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `does not allow autoplay if settings are turned off`() {
        mockSettings(featureEnabled = true, autoplaySettingEnabled = false, playAllSettingEnabled = false)
        val subject = AutoPlayConfig(featureConfig, sharedPrefs)
        assertFalse(subject.isAutoPlayEnabled(0))
    }

    @Test
    fun `does not allow autoplay if not in the test group`() {
        mockSettings(featureEnabled = false, autoplaySettingEnabled = true, playAllSettingEnabled = true)
        val subject = AutoPlayConfig(featureConfig, sharedPrefs)
        assertFalse(subject.isAutoPlayEnabled(0))
    }

    @Test
    fun `allows autoplay for the top item if play all is turned off`() {
        mockSettings(featureEnabled = true, autoplaySettingEnabled = true, playAllSettingEnabled = false)
        val subject = AutoPlayConfig(featureConfig, sharedPrefs)
        assertTrue(subject.isAutoPlayEnabled(0))
    }

    @Test
    fun `allows autoplay for all items if play all is turned on`() {
        mockSettings(featureEnabled = true, autoplaySettingEnabled = true, playAllSettingEnabled = true)
        val subject = AutoPlayConfig(featureConfig, sharedPrefs)
        assertTrue(subject.isAutoPlayEnabled(0))
        assertTrue(subject.isAutoPlayEnabled(1))
        assertTrue(subject.isAutoPlayEnabled(2))
        assertTrue(subject.isAutoPlayEnabled(3))
        assertTrue(subject.isAutoPlayEnabled(5))
        assertTrue(subject.isAutoPlayEnabled(8))
        assertTrue(subject.isAutoPlayEnabled(13))
        assertTrue(subject.isAutoPlayEnabled(21))
    }

    @Test
    fun `does not allow autoplay for non-top items if play all is turned off`() {
        mockSettings(featureEnabled = true, autoplaySettingEnabled = true, playAllSettingEnabled = false)
        val subject = AutoPlayConfig(featureConfig, sharedPrefs)
        assertTrue(subject.isAutoPlayEnabled(0))
        assertFalse(subject.isAutoPlayEnabled(1))
        assertFalse(subject.isAutoPlayEnabled(2))
        assertFalse(subject.isAutoPlayEnabled(3))
        assertFalse(subject.isAutoPlayEnabled(5))
        assertFalse(subject.isAutoPlayEnabled(8))
        assertFalse(subject.isAutoPlayEnabled(13))
        assertFalse(subject.isAutoPlayEnabled(21))
    }

    private fun mockSettings(
        featureEnabled: Boolean,
        autoplaySettingEnabled: Boolean,
        playAllSettingEnabled: Boolean
    ) {
        every { featureConfig.isFeatureEnabled(Feature.LIVE_IN_THE_LOBBY) } returns featureEnabled
        every { sharedPrefs.getBoolean("autoplay", any()) } returns autoplaySettingEnabled
        every { sharedPrefs.getBoolean("play_all", any()) } returns playAllSettingEnabled
    }
}
