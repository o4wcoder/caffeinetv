package tv.caffeine.app.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import timber.log.Timber
import tv.caffeine.app.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        findPreference(rootKey)?.title?.let { title ->
            (activity as? AppCompatActivity)?.supportActionBar?.title = title
        }
        configureLegalDocs()
    }

    private fun configureLegalDocs() {
        findPreference("tos")?.setOnPreferenceClickListener {
            openLegalDoc(LegalDoc.TOS)
        }
        findPreference("privacy")?.setOnPreferenceClickListener {
            openLegalDoc(LegalDoc.PrivacyPolicy)
        }
        findPreference("guidelines")?.setOnPreferenceClickListener {
            openLegalDoc(LegalDoc.CommunityGuidelines)
        }
        findPreference("ignored_users")?.setOnPreferenceClickListener {
            openIgnoredUsers()
        }
    }

    private fun openLegalDoc(legalDoc: LegalDoc): Boolean {
        val action = SettingsFragmentDirections.actionSettingsFragmentToLegalDocsFragment(legalDoc.ordinal)
        findNavController().navigate(action)
        return true
    }

    private fun openIgnoredUsers(): Boolean {
        findNavController().navigate(R.id.action_settingsFragment_to_ignoredUsersFragment)
        return true
    }

    override fun onNavigateToScreen(preferenceScreen: PreferenceScreen?) {
        super.onNavigateToScreen(preferenceScreen)
        preferenceScreen?.key?.let { key ->
            val args = Bundle().apply { putString(ARG_PREFERENCE_ROOT, key) }
            findNavController().navigate(R.id.action_settingsFragment_self, args)
        }
        Timber.d("Navigating to ${preferenceScreen?.title}")
    }

}
