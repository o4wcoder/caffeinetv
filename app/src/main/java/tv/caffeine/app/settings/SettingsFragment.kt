package tv.caffeine.app.settings

import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import timber.log.Timber
import tv.caffeine.app.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        findPreference("tos").setOnPreferenceClickListener {
            openLegalDoc(LegalDoc.TOS)
            return@setOnPreferenceClickListener true
        }
        findPreference("privacy").setOnPreferenceClickListener {
            openLegalDoc(LegalDoc.PrivacyPolicy)
            return@setOnPreferenceClickListener true
        }
        findPreference("guidelines").setOnPreferenceClickListener {
            openLegalDoc(LegalDoc.CommunityGuidelines)
            return@setOnPreferenceClickListener true
        }
    }

    private fun openLegalDoc(legalDoc: LegalDoc) {
        val action = SettingsFragmentDirections.actionSettingsFragmentToLegalDocsFragment(legalDoc.ordinal)
        findNavController().navigate(action)
    }
}
