package tv.caffeine.app.settings

import android.content.SharedPreferences
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import tv.caffeine.app.feature.Feature
import tv.caffeine.app.feature.FeatureConfig

class ReleaseDesignConfigTests {
    @MockK lateinit var featureConfig: FeatureConfig
    @MockK lateinit var sharedPrefs: SharedPreferences

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `does not allow release design if settings are turned off`() {
        mockSettings(featureEnabled = true, releaseDesignSettingEnabled = false)
        val subject = ReleaseDesignConfig(featureConfig, sharedPrefs)
        Assert.assertFalse(subject.isReleaseDesignActive())
    }

    @Test
    fun `does not allow release design if not in the test group`() {
        mockSettings(featureEnabled = false, releaseDesignSettingEnabled = false)
        val subject = ReleaseDesignConfig(featureConfig, sharedPrefs)
        Assert.assertFalse(subject.isReleaseDesignActive())
    }

    @Test
    fun `does not allow release design if not in the test group even if setting is enabled`() {
        mockSettings(featureEnabled = false, releaseDesignSettingEnabled = true)
        val subject = ReleaseDesignConfig(featureConfig, sharedPrefs)
        Assert.assertFalse(subject.isReleaseDesignActive())
    }

    @Test
    fun `allows release design if feature is enabled and setting is toggled on`() {
        mockSettings(featureEnabled = true, releaseDesignSettingEnabled = true)
        val subject = ReleaseDesignConfig(featureConfig, sharedPrefs)
        Assert.assertTrue(subject.isReleaseDesignActive())
    }

    private fun mockSettings(
        featureEnabled: Boolean,
        releaseDesignSettingEnabled: Boolean
    ) {
        every { featureConfig.isFeatureEnabled(Feature.RELEASE_DESIGN) } returns featureEnabled
        every { sharedPrefs.getBoolean("release_design", any()) } returns releaseDesignSettingEnabled
    }
}
