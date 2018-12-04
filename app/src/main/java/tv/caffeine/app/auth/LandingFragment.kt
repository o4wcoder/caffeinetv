package tv.caffeine.app.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.*
import tv.caffeine.app.databinding.FragmentLandingBinding
import tv.caffeine.app.ui.CaffeineFragment
import javax.inject.Inject


class LandingFragment : CaffeineFragment() {

    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var oauthService: OAuthService
    @Inject lateinit var gson: Gson
    @Inject lateinit var authWatcher: AuthWatcher

    private lateinit var binding: FragmentLandingBinding
    private lateinit var callbackManager: CallbackManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentLandingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.newAccountButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.signUpFragment))
        binding.signInWithEmailButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.signInFragment))
        callbackManager = CallbackManager.Factory.create()
        binding.facebookSignInButton.registerCallback(callbackManager, facebookCallback)
        LandingFragmentArgs.fromBundle(arguments).message?.let {
            Snackbar.make(view, it, Snackbar.LENGTH_SHORT).show()
        }
    }

    private val facebookCallback = object : FacebookCallback<LoginResult?> {
        override fun onSuccess(result: LoginResult?) {
            processFacebookLogin(result)
        }

        override fun onCancel() {
        }

        override fun onError(error: FacebookException?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    private fun processFacebookLogin(result: LoginResult?) {
        val token = result?.accessToken?.token ?: return
        launch {
            val deferred = oauthService.submitFacebookToken(FacebookTokenBody(token))
            val oauthCallbackResult = deferred.await()
            if (oauthCallbackResult.oauth != null) {
                continueToSignUp(oauthCallbackResult)
            } else if (oauthCallbackResult.next != null) {
                when(oauthCallbackResult.next) {
                    NextAccountAction.mfa_otp_required -> continueToMfaCode(oauthCallbackResult)
                    else -> attemptSignIn(oauthCallbackResult)
                }
            }
        }
    }

    private suspend fun attemptSignIn(oauthCallbackResult: OAuthCallbackResult) {
        val caid = oauthCallbackResult.caid
        val loginToken = oauthCallbackResult.loginToken
        val signInBody = SignInBody(Account(null, null, caid, loginToken))
        val response = accountsService.signIn(signInBody).await()
        val signInResult = response.body()
        when {
            response.isSuccessful && signInResult != null -> onSuccess(signInResult)
            else -> onError(response)
        }
    }

    private fun continueToSignUp(oauthCallbackResult: OAuthCallbackResult) {
        val action = LandingFragmentDirections.actionLandingFragmentToSignUpFragment()
        action.setOauthCallbackResult(oauthCallbackResult)
        findNavController().navigate(action)
    }

    private fun continueToMfaCode(oauthCallbackResult: OAuthCallbackResult) {
        val action = LandingFragmentDirections.actionLandingFragmentToMfaCodeFragment(null, null, oauthCallbackResult.caid, oauthCallbackResult.loginToken)
        findNavController().navigate(action)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    @UiThread
    private fun onSuccess(signInResult: SignInResult) {
        val navController = findNavController()
        tokenStore.storeSignInResult(signInResult)
        navController.popBackStack(R.id.landingFragment, true)
        navController.navigate(R.id.lobbyFragment)
        authWatcher.onSignIn()
    }

    @UiThread
    private fun onError(response: Response<SignInResult>) {
        val errorBody = response.errorBody() ?: return
        val error = gson.fromJson(errorBody.string(), ApiErrorResult::class.java)
        Timber.d("Error: $error")
        val errors = error.errors
        binding.formErrorTextView.text = listOfNotNull(errors._error, errors.username, errors.password)
                .joinToString("\n") { it.joinToString("\n") }
    }
}
