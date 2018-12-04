package tv.caffeine.app.settings

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.google.gson.Gson
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.NotificationSettings
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.User
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.di.ViewModelFactory
import tv.caffeine.app.profile.DeleteAccountDialogFragment
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import java.lang.Exception
import javax.inject.Inject

class SettingsFragment : PreferenceFragmentCompat(), HasSupportFragmentInjector {
    @Inject lateinit var childFragmentInjector: DispatchingAndroidInjector<Fragment>
    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModelProvider by lazy { ViewModelProviders.of(this, viewModelFactory) }
    private val viewModel by lazy { viewModelProvider.get(SettingsViewModel::class.java) }
    private lateinit var notificationSettingsViewModel: NotificationSettingsViewModel

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = childFragmentInjector

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        findPreference(rootKey)?.title?.let { title ->
            (activity as? AppCompatActivity)?.supportActionBar?.title = title
        }
        configureAuthSettings()
        configureNotificationSettings()
        configureLegalDocs()
        configureIgnoredUsers()
        configureSocialAccounts()
        configureDeleteAccount()
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        notificationSettingsViewModel = ViewModelProviders.of(context as FragmentActivity, viewModelFactory)
                .get(NotificationSettingsViewModel::class.java)
    }

    override fun onStop() {
        updateNotificationSettings()
        super.onStop()
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

    private fun configureNotificationSettings() {
        findPreference("push_notifications")?.let { preference ->
            notificationSettingsViewModel.pushCount.observe(this, Observer { count ->
                preference.summary = count.toString()
            })
        }
        findPreference("email_notifications")?.let { preference ->
            notificationSettingsViewModel.emailCount.observe(this, Observer { count ->
                preference.summary = count.toString()
            })
        }
        notificationSettingsViewModel.notificationSettings.observe(this, Observer { settings ->
            settings.toMap().forEach {
                (findPreference(it.key.name) as? CheckBoxPreference)?.isChecked = (it.value == true)
            }
        })
        // Since the view model is bound to the activity's scope to share data between fragments, we manually load
        // every time [SettingsFragment] is created, rather than only once when the view model is initialized.
        notificationSettingsViewModel.load()
    }

    private fun updateNotificationSettings() {
        val newSettings = NotificationSettings.SettingKey.values().associate {
            it to (findPreference(it.name) as? CheckBoxPreference)?.isChecked
        }
        notificationSettingsViewModel.remoteSave(newSettings)
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
                val twitter = user.connectedAccounts?.get("twitter")
                @StringRes val title = if (twitter != null) R.string.disconnect_twitter_account else R.string.connect_twitter_account
                preference.title = getString(title)
            })
        }
        findPreference("manage_facebook_account")?.let { preference ->
            viewModel.userDetails.observe(this, Observer {  user ->
                val twitter = user.connectedAccounts?.get("facebook")
                @StringRes val title = if (twitter != null) R.string.disconnect_facebook_account else R.string.connect_facebook_account
                preference.title = getString(title)
            })
        }
    }

    private fun configureDeleteAccount() {
        findPreference("delete_caffeine_account")?.setOnPreferenceClickListener {
            viewModel.userDetails.observe(this,  Observer { user ->
                fragmentManager?.let { fm ->
                    DeleteAccountDialogFragment().apply {
                        arguments = SettingsFragmentDirections.actionSettingsFragmentToDeleteAccountDialogFragment(user.username).arguments
                        show(fm, "deleteAccount")
                    }
                }
            })
            true
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
        dispatchConfig: DispatchConfig,
        private val tokenStore: TokenStore,
        private val followManager: FollowManager
) : CaffeineViewModel(dispatchConfig) {
    private val _userDetails = MutableLiveData<User>()
    val userDetails: LiveData<User> = Transformations.map(_userDetails) { it }

    init {
        load()
    }

    private fun load() {
        val caid = tokenStore.caid ?: return
        launch {
            val userDetails = followManager.userDetails(caid)
            _userDetails.value = userDetails
        }
    }
}

class NotificationSettingsViewModel(
        dispatchConfig: DispatchConfig,
        private val accountsService: AccountsService,
        private val gson: Gson
): CaffeineViewModel(dispatchConfig) {

    private val _notificationSettings = MutableLiveData<NotificationSettings>()
    val notificationSettings: LiveData<NotificationSettings> = Transformations.map(_notificationSettings) { it }
    val pushCount:LiveData<Int> = Transformations.map(_notificationSettings) { getPushCount(it) }
    val emailCount:LiveData<Int> = Transformations.map(_notificationSettings) { getEmailCount(it) }

    fun load() {
        launch {
            val result = accountsService.getNotificationSettings().awaitAndParseErrors(gson)
            when (result) {
                is CaffeineResult.Success -> _notificationSettings.value = result.value
                is CaffeineResult.Error -> Timber.e(Exception(result.error.toString()), "Failed to load notification settings")
                is CaffeineResult.Failure -> Timber.e(result.exception, "Failed to load notification settings")
            }
        }
    }

    fun remoteSave(newSettings: Map<NotificationSettings.SettingKey, Boolean?>) {
        var changed = false
        val oldSettings = _notificationSettings.value?.toMap() ?: mutableMapOf()
        for (key in newSettings.keys) {
            // Null is expected. E.g., push notification views are unavailable when the settings root is email.
            // Do not overwrite the old settings map in these cases.
            newSettings[key]?.let {
                if (it != oldSettings[key]) {
                    oldSettings[key] = it
                    changed = true
                }
            }
        }

        if (changed) {
            launch {
                val result = accountsService.updateNotificationSettings(NotificationSettings.fromMap(oldSettings))
                        .awaitAndParseErrors(gson)
                when (result) {
                    is CaffeineResult.Success -> _notificationSettings.value = result.value
                    is CaffeineResult.Error -> Timber.e(Exception(result.error.toString()), "Failed to save notification settings")
                    is CaffeineResult.Failure -> Timber.e(result.exception, "Failed to save notification settings")
                }
            }
        }
    }

    private fun getPushCount(settings: NotificationSettings): Int {
        return listOf(settings.newFollowerAndroidPush, settings.broadcastLiveAndroidPush,
                settings.watchingBroadcastAndroidPush, settings.friendJoinsAndroidPush)
                .count { it == true }
    }

    private fun getEmailCount(settings: NotificationSettings): Int {
        return listOf(settings.newFollowerEmail, settings.weeklySuggestionsEmail,
                settings.broadcastLiveEmail, settings.friendJoinsEmail,
                settings.watchingBroadcastEmail, settings.communityEmail, settings.broadcastReportEmail)
                .count { it == true }
    }
}
