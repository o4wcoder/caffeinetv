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
    fun `allows release design if the dev option is disabled and the settings is enabled`() {
        mockSettings(devOptionsEnabled = false, releaseDesignSettingEnabled = true)
        val subject = ReleaseDesignConfig(featureConfig, sharedPrefs)
        Assert.assertTrue(subject.isReleaseDesignActive())
    }

    @Test
    fun `allows release design if the dev option is disabled and the settings is disabled`() {
        mockSettings(devOptionsEnabled = false, releaseDesignSettingEnabled = false)
        val subject = ReleaseDesignConfig(featureConfig, sharedPrefs)
        Assert.assertTrue(subject.isReleaseDesignActive())
    }

    @Test
    fun `allows release design if dev options is enabled and setting is enabled`() {
        mockSettings(devOptionsEnabled = true, releaseDesignSettingEnabled = true)
        val subject = ReleaseDesignConfig(featureConfig, sharedPrefs)
        Assert.assertTrue(subject.isReleaseDesignActive())
    }

    @Test
    fun `does not allow release design if dev options is enabled and setting is disabled`() {
        mockSettings(devOptionsEnabled = true, releaseDesignSettingEnabled = false)
        val subject = ReleaseDesignConfig(featureConfig, sharedPrefs)
        Assert.assertFalse(subject.isReleaseDesignActive())
    }

    private fun mockSettings(
        devOptionsEnabled: Boolean,
        releaseDesignSettingEnabled: Boolean
    ) {
        every { featureConfig.isFeatureEnabled(Feature.DEV_OPTIONS) } returns devOptionsEnabled
        every { sharedPrefs.getBoolean("release_design", any()) } returns releaseDesignSettingEnabled
    }
}
