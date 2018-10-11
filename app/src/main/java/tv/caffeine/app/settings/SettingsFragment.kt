package tv.caffeine.app.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import tv.caffeine.app.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }

}
