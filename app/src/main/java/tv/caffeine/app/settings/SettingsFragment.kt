package tv.caffeine.app.settings

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.model.User
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.di.ViewModelFactory
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineViewModel
import javax.inject.Inject

class SettingsFragment : PreferenceFragmentCompat(), HasSupportFragmentInjector {
    @Inject lateinit var childFragmentInjector: DispatchingAndroidInjector<Fragment>

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = childFragmentInjector

    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModelProvider by lazy { ViewModelProviders.of(this, viewModelFactory) }
    private val viewModel by lazy { viewModelProvider.get(SettingsViewModel::class.java) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        findPreference(rootKey)?.title?.let { title ->
            (activity as? AppCompatActivity)?.supportActionBar?.title = title
        }
        configureAuthSettings()
        configureLegalDocs()
        configureIgnoredUsers()
        configureSocialAccounts()
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    private fun configureAuthSettings() {
        findPreference("change_email")?.setOnPreferenceClickListener {
            findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToUpdateEmailFragment())
            true
        }
        findPreference("change_password")?.setOnPreferenceClickListener {
            findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToUpdatePasswordFragment())
            true
        }
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
    }

    private fun configureIgnoredUsers() {
        findPreference("ignored_users")?.setOnPreferenceClickListener {
            openIgnoredUsers()
        }
    }

    private fun configureSocialAccounts() {
        findPreference("manage_twitter_account")?.let { preference ->
            viewModel.userDetails.observe(this, Observer {  user ->
                val twitter = user.connectedAccounts["twitter"]
                @StringRes val title = if (twitter != null) R.string.disconnect_twitter_account else R.string.connect_twitter_account
                preference.title = getString(title)
            })
        }
        findPreference("manage_facebook_account")?.let { preference ->
            viewModel.userDetails.observe(this, Observer {  user ->
                val twitter = user.connectedAccounts["facebook"]
                @StringRes val title = if (twitter != null) R.string.disconnect_facebook_account else R.string.connect_facebook_account
                preference.title = getString(title)
            })
        }
    }

    private fun openLegalDoc(legalDoc: LegalDoc): Boolean {
        val action = SettingsFragmentDirections.actionSettingsFragmentToLegalDocsFragment(legalDoc)
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

class SettingsViewModel(
        private val tokenStore: TokenStore,
        private val followManager: FollowManager
) : CaffeineViewModel() {
    private val _userDetails = MutableLiveData<User>()
    val userDetails: LiveData<User> get() = _userDetails

    init {
        load()
    }

    private fun load() {
        val caid = tokenStore.caid ?: return
        launch {
            val userDetails = followManager.userDetails(caid)
            withContext(Dispatchers.Main) {
                _userDetails.value = userDetails
            }
        }
    }
}
