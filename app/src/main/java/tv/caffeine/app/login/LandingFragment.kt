package tv.caffeine.app.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.analytics.Analytics
import tv.caffeine.app.analytics.AnalyticsEvent
import tv.caffeine.app.analytics.FirebaseEvent
import tv.caffeine.app.analytics.logEvent
import tv.caffeine.app.analytics.logScreen
import tv.caffeine.app.api.Account
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.CaffeineCredentials
import tv.caffeine.app.api.FacebookTokenBody
import tv.caffeine.app.api.NextAccountAction
import tv.caffeine.app.api.OAuthCallbackResult
import tv.caffeine.app.api.OAuthService
import tv.caffeine.app.api.SignInBody
import tv.caffeine.app.api.SignInResult
import tv.caffeine.app.api.generalErrorsString
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.IdentityProvider
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.api.passwordErrorsString
import tv.caffeine.app.api.usernameErrorsString
import tv.caffeine.app.auth.AuthWatcher
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.databinding.FragmentLandingBinding
import tv.caffeine.app.social.TwitterAuthViewModel
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.util.showSnackbar
import javax.inject.Inject
import tv.caffeine.app.R
import tv.caffeine.app.util.getAssetFile

class LandingFragment @Inject constructor(
    private val accountsService: AccountsService,
    private val tokenStore: TokenStore,
    private val oauthService: OAuthService,
    private val gson: Gson,
    private val authWatcher: AuthWatcher,
    @VisibleForTesting var analytics: Analytics,
    private val firebaseAnalytics: FirebaseAnalytics,
    private val facebookLoginManager: LoginManager
) : CaffeineFragment(R.layout.fragment_landing) {

    private lateinit var binding: FragmentLandingBinding
    private lateinit var callbackManager: CallbackManager
    private val args by navArgs<LandingFragmentArgs>()
    private val twitterAuth: TwitterAuthViewModel by navGraphViewModels(R.id.login) { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        callbackManager = CallbackManager.Factory.create()
        facebookLoginManager.registerCallback(callbackManager, facebookCallback)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        firebaseAnalytics.logScreen(this)
        binding = FragmentLandingBinding.bind(view)
        binding.newAccountButton.setOnClickListener {
            analytics.trackEvent(AnalyticsEvent.NewAccountClicked)
            firebaseAnalytics.logEvent(FirebaseEvent.NewAccountClicked)
            val action = LandingFragmentDirections.actionLandingFragmentToSignUpFragment()
            findNavController().safeNavigate(action)
        }
        View.OnClickListener {
            firebaseAnalytics.logEvent(FirebaseEvent.SignInClicked)
            findNavController().safeNavigate(LandingFragmentDirections.actionLandingFragmentToSignInFragment())
        }.let {
            binding.signInWithUsernameTextView.setOnClickListener(it)
        }
        binding.facebookSignInButton.setOnClickListener {
            analytics.trackEvent(AnalyticsEvent.SocialSignInClicked(IdentityProvider.facebook))
            firebaseAnalytics.logEvent(FirebaseEvent.ContinueWithFacebookClicked)
            facebookLoginManager.logInWithReadPermissions(this, resources.getStringArray(R.array.facebook_permissions).toList())
        }
        binding.twitterSignInButton.setOnClickListener {
            analytics.trackEvent(AnalyticsEvent.SocialSignInClicked(IdentityProvider.twitter))
            firebaseAnalytics.logEvent(FirebaseEvent.ContinueWithTwitterClicked)
            findNavController().navigate(LandingFragmentDirections.actionLandingFragmentToTwitterAuthFragment())
        }
        twitterAuth.oauthResult.observe(viewLifecycleOwner, Observer { event ->
            event.getContentIfNotHandled()?.let { result -> processTwitterOAuthResult(result) }
        })
        args.message?.let {
            Snackbar.make(view, it, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadSplashVideo()
    }

    private fun loadSplashVideo() {
        val videoView = binding.splashVideoView
        val videoUri = activity?.getAssetFile(R.raw.caffeine_mobile_splash)
        videoView.setVideoURI(videoUri)
        videoView.start()
        videoView.setOnPreparedListener { it.isLooping = true }
    }

    private val facebookCallback = object : FacebookCallback<LoginResult?> {
        override fun onSuccess(result: LoginResult?) {
            processFacebookLogin(result)
        }

        override fun onCancel() {
        }

        override fun onError(error: FacebookException?) {
            activity?.showSnackbar(R.string.error_facebook_callback)
        }
    }

    private fun processTwitterOAuthResult(result: CaffeineResult<OAuthCallbackResult>) {
        when (result) {
            is CaffeineResult.Success -> {
                Timber.d("Twitter OAuth login success, ${result.value}")
                processOAuthResult(result.value, IdentityProvider.twitter)
            }
            is CaffeineResult.Error -> {
                activity?.showSnackbar(R.string.twitter_login_failed)
                Timber.e("Error logging in with Twitter ${result.error}")
            }
            is CaffeineResult.Failure -> {
                activity?.showSnackbar(R.string.twitter_login_failed)
                Timber.e(result.throwable)
            }
        }
    }

    private fun processFacebookLogin(loginResult: LoginResult?) {
        val token = loginResult?.accessToken?.token ?: return
        launch {
            val deferred = oauthService.submitFacebookToken(FacebookTokenBody(token))
            val result = deferred.awaitAndParseErrors(gson)
            when (result) {
                is CaffeineResult.Success -> processOAuthResult(result.value, IdentityProvider.facebook)
            }
        }
    }

    private fun processOAuthResult(oauthCallbackResult: OAuthCallbackResult, identityProvider: IdentityProvider) {
        when {
            oauthCallbackResult.credentials != null -> {
                when (identityProvider) {
                    IdentityProvider.facebook -> firebaseAnalytics.logEvent(FirebaseEvent.FacebookSignInSuccess)
                    IdentityProvider.twitter -> firebaseAnalytics.logEvent(FirebaseEvent.TwitterSignInSuccess)
                }
                onSuccess(oauthCallbackResult.credentials)
            }
            oauthCallbackResult.next == NextAccountAction.mfa_otp_required -> {
                when (identityProvider) {
                    IdentityProvider.facebook -> firebaseAnalytics.logEvent(FirebaseEvent.FacebookContinueToMFA)
                    IdentityProvider.twitter -> firebaseAnalytics.logEvent(FirebaseEvent.TwitterContinueToMFA)
                }
                continueToMfaCode(oauthCallbackResult)
            }
            oauthCallbackResult.oauth != null -> {
                when (identityProvider) {
                    IdentityProvider.facebook -> firebaseAnalytics.logEvent(FirebaseEvent.FacebookContinueToSignUp)
                    IdentityProvider.twitter -> firebaseAnalytics.logEvent(FirebaseEvent.TwitterContinueToSignUp)
                }
                continueToSignUp(oauthCallbackResult)
            }
            else -> {
                firebaseAnalytics.logEvent(FirebaseEvent.SocialOAuthEdgeCase)
                attemptSignIn(oauthCallbackResult)
            }
        }
    }

    private fun attemptSignIn(oauthCallbackResult: OAuthCallbackResult) = launch {
        val caid = oauthCallbackResult.caid
        val loginToken = oauthCallbackResult.loginToken
        val signInBody = SignInBody(Account(null, null, caid, loginToken))
        val result = accountsService.signIn(signInBody).awaitAndParseErrors(gson)
        when (result) {
            is CaffeineResult.Success -> onSuccess(result.value)
            is CaffeineResult.Error -> onError(result.error)
            is CaffeineResult.Failure -> handleFailure(result)
        }
    }

    private fun continueToSignUp(oauthCallbackResult: OAuthCallbackResult) {
        val action = LandingFragmentDirections.actionLandingFragmentToSignUpFragment(oauthCallbackResult.possibleUsername, oauthCallbackResult.oauth?.email, oauthCallbackResult.oauth?.iid)
        findNavController().safeNavigate(action)
    }

    private fun continueToMfaCode(oauthCallbackResult: OAuthCallbackResult) {
        val action = LandingFragmentDirections.actionLandingFragmentToMfaCodeFragment(null, null, oauthCallbackResult.caid, oauthCallbackResult.loginToken)
        findNavController().safeNavigate(action)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    @UiThread
    private fun onSuccess(credentials: CaffeineCredentials) {
        val navController = findNavController()
        tokenStore.storeCredentials(credentials)
        navController.popBackStack(R.id.landingFragment, true)
        navController.safeNavigate(R.id.lobbySwipeFragment)
        authWatcher.onSignIn()
    }

    @UiThread
    private fun onSuccess(signInResult: SignInResult) {
        val navController = findNavController()
        tokenStore.storeSignInResult(signInResult)
        navController.popBackStack(R.id.landingFragment, true)
        navController.safeNavigate(R.id.lobbySwipeFragment)
        authWatcher.onSignIn()
    }

    @UiThread
    private fun onError(error: ApiErrorResult) {
        Timber.d("Error: $error")
        showSnackbar(listOfNotNull(error.generalErrorsString, error.usernameErrorsString, error.passwordErrorsString)
                .joinToString("\n"))
    }
}
