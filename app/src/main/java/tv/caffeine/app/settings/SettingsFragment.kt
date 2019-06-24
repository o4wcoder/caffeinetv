package tv.caffeine.app.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.gson.Gson
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.FacebookTokenBody
import tv.caffeine.app.api.NotificationSettings
import tv.caffeine.app.api.OAuthCallbackResult
import tv.caffeine.app.api.OAuthService
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.isIdentityRateLimitExceeded
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.IdentityProvider
import tv.caffeine.app.api.model.MfaMethod
import tv.caffeine.app.api.model.User
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.di.ViewModelFactory
import tv.caffeine.app.feature.Feature
import tv.caffeine.app.feature.FeatureConfig
import tv.caffeine.app.profile.DeleteAccountDialogFragment
import tv.caffeine.app.profile.MyProfileViewModel
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.social.TwitterAuthViewModel
import tv.caffeine.app.ui.AlertDialogFragment
import tv.caffeine.app.util.maybeShow
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.util.showSnackbar
import javax.inject.Inject
import kotlin.collections.set

private const val DISCONNECT_IDENTITY = 1

class SettingsFragment @Inject constructor(
    private val childFragmentInjector: DispatchingAndroidInjector<Fragment>,
    private val viewModelFactory: ViewModelFactory,
    private val facebookLoginManager: LoginManager,
    private val featureConfig: FeatureConfig
) : PreferenceFragmentCompat(), HasSupportFragmentInjector, DisconnectIdentityDialogFragment.Callback {

    private val viewModel: SettingsViewModel by viewModels { viewModelFactory }
    private val myProfileViewModel: MyProfileViewModel by viewModels { viewModelFactory }
    private val twitterAuth: TwitterAuthViewModel by navGraphViewModels(R.id.settings) { viewModelFactory }
    private val notificationSettingsViewModel: NotificationSettingsViewModel by activityViewModels { viewModelFactory }

    private val callbackManager: CallbackManager = CallbackManager.Factory.create()

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
        if (rootKey == null && featureConfig.isFeatureEnabled(Feature.LIVE_IN_THE_LOBBY)) {
            addPreferencesFromResource(R.xml.f316)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        twitterAuth.oauthResult.observe(this, Observer { event ->
            event.getContentIfNotHandled()?.let { result -> processTwitterOAuthResult(result) }
        })
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onStop() {
        updateNotificationSettings()
        super.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun configureAuthSettings() {
        findPreference("change_email")?.setOnPreferenceClickListener {
            findNavController().safeNavigate(SettingsFragmentDirections.actionSettingsFragmentToUpdateEmailFragment())
            true
        }
        findPreference("change_password")?.setOnPreferenceClickListener {
            findNavController().safeNavigate(SettingsFragmentDirections.actionSettingsFragmentToUpdatePasswordFragment())
            true
        }

        // TODO: Remove feature flag when feature is complete
        val showTwoStepAuthSetting = false
        myProfileViewModel.userProfile.observe(this, Observer { userProfile ->
            @StringRes val status = when (userProfile.mfaMethod) {
                MfaMethod.EMAIL, MfaMethod.TOTP -> R.string.mfa_status_on
                else -> R.string.mfa_status_off
            }
            findPreference("manage_2fa")?.apply {
                setSummary(status)
                if (!showTwoStepAuthSetting) {
                    setOnPreferenceClickListener {
                        val fragment = AlertDialogFragment.withMessage(R.string.manage_mfa_coming_soon)
                        fragment.maybeShow(fragmentManager, "mfaSoonDialog")
                        true
                    }
                }
            }
            findPreference("manage_2fa_update")?.apply {
                if (!showTwoStepAuthSetting) {
                    this.isVisible = false
                } else {
                    setOnPreferenceChangeListener { _, newValue ->
                        if (newValue as Boolean) {
                            viewModel.sendMTAEmailCode()
                            findNavController().safeNavigate(
                                SettingsFragmentDirections.actionSettingsFragmentToTwoStepAuthFragment(
                                    userProfile.email ?: getString(R.string.email)))
                        } else {
                            findNavController().safeNavigate(R.id.twoStepAuthDisableDialogFragment)
                        }
                        true
                    }
                }
            }
            findPreference("change_email")?.summary = userProfile.email ?: getString(R.string.email)
        })
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

    private fun processTwitterOAuthResult(result: CaffeineResult<OAuthCallbackResult>) {
        when (result) {
            is CaffeineResult.Success -> viewModel.processTwitterAuth(result.value)
            is CaffeineResult.Error -> {
                Timber.e("Error connecting Twitter, ${result.error}")
                activity?.showSnackbar(R.string.twitter_login_failed)
            }
            is CaffeineResult.Failure -> {
                Timber.e(result.throwable)
                activity?.showSnackbar(R.string.twitter_login_failed)
            }
        }
    }

    private fun configureSocialAccounts() {
        findPreference("manage_twitter_account")?.let { preference ->
            viewModel.userDetails.observe(this, Observer { user ->
                val twitter = user?.connectedAccounts?.get("twitter")
                @StringRes val title = if (twitter != null) R.string.disconnect_twitter_account else R.string.connect_twitter_account
                preference.title = getString(title)
                preference.isEnabled = user != null
                if (user == null) return@Observer
                preference.setOnPreferenceClickListener {
                    if (twitter != null) {
                        val action = SettingsFragmentDirections.actionSettingsFragmentToDisconnectIdentityDialogFragment(twitter.uid, IdentityProvider.twitter, twitter.displayName)
                        val fragment = DisconnectIdentityDialogFragment()
                        fragment.arguments = action.arguments
                        fragment.setTargetFragment(this, DISCONNECT_IDENTITY)
                        fragment.maybeShow(fragmentManager, "disconnectTwitter")
                    } else {
                        findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToTwitterAuthFragment())
                    }
                    true
                }
            })
        }
        findPreference("manage_facebook_account")?.let { preference ->
            viewModel.userDetails.observe(this, Observer { user ->
                val facebook = user?.connectedAccounts?.get("facebook")
                @StringRes val title = if (facebook != null) R.string.disconnect_facebook_account else R.string.connect_facebook_account
                preference.title = getString(title)
                preference.isEnabled = user != null
                if (user == null) return@Observer
                preference.setOnPreferenceClickListener {
                    if (facebook != null) {
                        val action = SettingsFragmentDirections.actionSettingsFragmentToDisconnectIdentityDialogFragment(facebook.uid, IdentityProvider.facebook, facebook.displayName)
                        val fragment = DisconnectIdentityDialogFragment()
                        fragment.arguments = action.arguments
                        fragment.setTargetFragment(this, DISCONNECT_IDENTITY)
                        fragment.maybeShow(fragmentManager, "disconnectFacebook")
                    } else {
                        facebookLoginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                            override fun onSuccess(result: LoginResult?) {
                                viewModel.processFacebookLogin(result)
                            }

                            override fun onCancel() {
                            }

                            override fun onError(error: FacebookException?) {
                                activity?.showSnackbar(R.string.error_facebook_callback)
                            }
                        })
                        facebookLoginManager.logInWithReadPermissions(this, resources.getStringArray(R.array.facebook_permissions).toList())
                    }
                    true
                }
            })
        }
    }

    override fun confirmDisconnectIdentity(socialUid: String, identityProvider: IdentityProvider) {
        viewModel.disconnectIdentity(identityProvider, socialUid).observe(this, Observer { result ->
            val snackbar = when {
                result is CaffeineEmptyResult.Success -> R.string.success_disconnecting_social_account
                result is CaffeineEmptyResult.Error && result.error.isIdentityRateLimitExceeded() -> R.string.social_account_rate_limit_exceeded
                else -> R.string.failure_disconnecting_social_account
            }
            activity?.showSnackbar(snackbar)
        })
    }

    private fun configureDeleteAccount() {
        findPreference("delete_caffeine_account")?.let { preference ->
            viewModel.userDetails.observe(this, Observer { user ->
                preference.isEnabled = user != null
                if (user == null) return@Observer
                preference.setOnPreferenceClickListener {
                    fragmentManager?.let { fm ->
                        DeleteAccountDialogFragment().apply {
                            arguments = SettingsFragmentDirections.actionSettingsFragmentToDeleteAccountDialogFragment(user.username).arguments
                            show(fm, "deleteAccount")
                        }
                    }
                    true
                }
            })
        }
    }

    private fun openLegalDoc(legalDoc: LegalDoc): Boolean {
        val action = SettingsFragmentDirections.actionSettingsFragmentToLegalDocsFragment(legalDoc)
        findNavController().safeNavigate(action)
        return true
    }

    private fun openIgnoredUsers(): Boolean {
        findNavController().safeNavigate(R.id.action_settingsFragment_to_ignoredUsersFragment)
        return true
    }

    override fun onNavigateToScreen(preferenceScreen: PreferenceScreen?) {
        super.onNavigateToScreen(preferenceScreen)
        preferenceScreen?.key?.let { key ->
            val args = Bundle().apply { putString(ARG_PREFERENCE_ROOT, key) }
            findNavController().safeNavigate(R.id.action_settingsFragment_self, args)
        }
        Timber.d("Navigating to ${preferenceScreen?.title}")
    }
}

class SettingsViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val followManager: FollowManager,
    private val usersService: UsersService,
    private val oauthService: OAuthService,
    private val facebookLoginManager: LoginManager,
    private val accountsService: AccountsService,
    private val gson: Gson
) : ViewModel() {
    private val _userDetails = MutableLiveData<User>()
    val userDetails: LiveData<User?> = _userDetails.map { it }

    init {
        load()
    }

    private fun load() {
        val caid = tokenStore.caid ?: return
        viewModelScope.launch {
            val userDetails = followManager.loadUserDetails(caid)
            _userDetails.value = userDetails
        }
    }

    fun processFacebookLogin(loginResult: LoginResult?) = viewModelScope.launch {
        val token = loginResult?.accessToken?.token ?: return@launch
        val deferred = oauthService.submitFacebookToken(FacebookTokenBody(token))
        val result = deferred.awaitAndParseErrors(gson)
        when (result) {
            is CaffeineResult.Success -> Timber.d("Successful Facebook login")
            is CaffeineResult.Error -> Timber.d("Error attempting Facebook login ${result.error}")
            is CaffeineResult.Failure -> Timber.d("Failure attempting Facebook login ${result.throwable}")
        }
        load()
    }

    fun processTwitterAuth(result: OAuthCallbackResult) {
        Timber.d("Successfully connected Twitter, $result")
        load()
    }

    fun disconnectIdentity(identityProvider: IdentityProvider, socialUid: String): LiveData<CaffeineEmptyResult> {
        val liveData = MutableLiveData<CaffeineEmptyResult>()
        viewModelScope.launch {
            val caid = tokenStore.caid ?: return@launch
            val result = usersService.disconnectIdentity(caid, socialUid, identityProvider).awaitEmptyAndParseErrors(gson)
            when (result) {
                is CaffeineEmptyResult.Success -> {
                    Timber.d("Successfully disconnected identity")
                    when (identityProvider) {
                        IdentityProvider.facebook -> {
                            facebookLoginManager.logOut()
                        }
                        IdentityProvider.twitter -> {
                            // TODO clear out web view cache, in case the user checked "remember me on this device"
                        }
                    }
                }
                is CaffeineEmptyResult.Error -> Timber.d("Error disconnecting identity ${result.error}")
                is CaffeineEmptyResult.Failure -> Timber.d("Failure disconnecting identity ${result.throwable}")
            }
            load()
            liveData.value = result
        }
        return liveData.map { it }
    }

    fun sendMTAEmailCode() {
        viewModelScope.launch {
            val result = accountsService.sendMFAEmailCode().awaitEmptyAndParseErrors(gson)

            when (result) {
                is CaffeineEmptyResult.Success -> Timber.d("Successful send MFA email code")
                is CaffeineEmptyResult.Error -> Timber.d("Error attempting to send MFA email code ${result.error}")
                is CaffeineEmptyResult.Failure -> Timber.d("Failure attempting to send MFA email code ${result.throwable}")
            }
        }
    }
}

class NotificationSettingsViewModel @Inject constructor(
    private val accountsService: AccountsService,
    private val gson: Gson
) : ViewModel() {

    private val _notificationSettings = MutableLiveData<NotificationSettings>()
    val notificationSettings: LiveData<NotificationSettings> = _notificationSettings.map { it }
    val pushCount: LiveData<Int> = _notificationSettings.map { getPushCount(it) }
    val emailCount: LiveData<Int> = _notificationSettings.map { getEmailCount(it) }

    fun load() {
        viewModelScope.launch {
            val result = accountsService.getNotificationSettings().awaitAndParseErrors(gson)
            when (result) {
                is CaffeineResult.Success -> _notificationSettings.value = result.value
                is CaffeineResult.Error -> Timber.e("Failed to load notification settings, ${result.error}")
                is CaffeineResult.Failure -> Timber.e(result.throwable, "Failed to load notification settings")
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
            viewModelScope.launch {
                val result = accountsService.updateNotificationSettings(NotificationSettings.fromMap(oldSettings))
                        .awaitAndParseErrors(gson)
                when (result) {
                    is CaffeineResult.Success -> _notificationSettings.value = result.value
                    is CaffeineResult.Error -> Timber.e("Failed to save notification settings, ${result.error}")
                    is CaffeineResult.Failure -> Timber.e(result.throwable, "Failed to save notification settings")
                }
            }
        }
    }

    private fun getPushCount(settings: NotificationSettings): Int {
        return listOf(settings.newFollowerAndroidPush, settings.broadcastLiveAndroidPush,
                settings.friendJoinsAndroidPush)
                .count { it == true }
    }

    private fun getEmailCount(settings: NotificationSettings): Int {
        return listOf(settings.newFollowerEmail, settings.weeklySuggestionsEmail,
                settings.broadcastLiveEmail, settings.friendJoinsEmail,
                settings.communityEmail, settings.broadcastReportEmail)
                .count { it == true }
    }
}
