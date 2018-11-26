package tv.caffeine.app.settings

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
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
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import java.lang.Exception
import javax.inject.Inject

class SettingsFragment : PreferenceFragmentCompat(), HasSupportFragmentInjector {
    @Inject lateinit var childFragmentInjector: DispatchingAndroidInjector<Fragment>

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = childFragmentInjector

    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModelProvider by lazy { ViewModelProviders.of(this, viewModelFactory) }
    private val viewModel by lazy { viewModelProvider.get(SettingsViewModel::class.java) }
    private val notificationSettingsViewModel by lazy { viewModelProvider.get(NotificationSettingsViewModel::class.java) }

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
            listOf(Pair("broadcast_live_android_push", settings.broadcastLiveAndroidPush),
                    Pair("watching_broadcast_android_push", settings.watchingBroadcastAndroidPush),
                    Pair("new_follower_android_push", settings.newFollowerAndroidPush),
                    Pair("friend_joins_android_push", settings.friendJoinsAndroidPush),
                    Pair("broadcast_live_email", settings.broadcastLiveEmail),
                    Pair("watching_broadcast_email", settings.watchingBroadcastEmail),
                    Pair("new_follower_email", settings.newFollowerEmail),
                    Pair("friend_joins_email", settings.friendJoinsEmail),
                    Pair("weekly_suggestions_email", settings.weeklySuggestionsEmail),
                    Pair("community_email", settings.communityEmail),
                    Pair("broadcast_report", settings.broadcastReportEmail)).forEach {
                (findPreference(it.first) as? CheckBoxPreference)?.isChecked = (it.second == true)
            }
        })
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

    init {
        load()
    }

    private fun load() {
        launch {
            val result = accountsService.getNotificationSettings().awaitAndParseErrors(gson)
            when (result) {
                is CaffeineResult.Success -> _notificationSettings.value = result.value
                is CaffeineResult.Error -> Timber.e(Exception(result.error.toString()), "Failed to load notification settings")
                is CaffeineResult.Failure -> Timber.e(result.exception, "Failed to load notification settings")
            }
        }
    }

    private fun getPushCount(settings: NotificationSettings): Int {
        return listOf(settings.newFollowerAndroidPush, settings.broadcastLiveAndroidPush,
                settings.watchingBroadcastAndroidPush, settings.friendJoinsAndroidPush)
                .sumBy { if (it == true) 1 else 0 }
    }

    private fun getEmailCount(settings: NotificationSettings): Int {
        return listOf(settings.newFollowerEmail, settings.weeklySuggestionsEmail,
                settings.broadcastLiveEmail, settings.friendJoinsEmail,
                settings.watchingBroadcastEmail, settings.communityEmail, settings.broadcastReportEmail)
                .sumBy { if (it == true) 1 else 0 }
    }
}
