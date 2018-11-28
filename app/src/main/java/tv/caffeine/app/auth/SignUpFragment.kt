package tv.caffeine.app.auth

import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.*
import tv.caffeine.app.databinding.FragmentSignUpBinding
import tv.caffeine.app.ui.CaffeineFragment
import javax.inject.Inject


class SignUpFragment : CaffeineFragment() {

    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var gson: Gson

    private lateinit var binding: FragmentSignUpBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.signUpButton.setOnClickListener { signUp() }
        binding.agreeToLegalCheckbox.apply {
            text = HtmlCompat.fromHtml(resources.getString(R.string.i_agree_to_legal), HtmlCompat.FROM_HTML_MODE_LEGACY)
            movementMethod = LinkMovementMethod.getInstance()
        }
        arguments?.let { SignUpFragmentArgs.fromBundle(it) }?.oauthCallbackResult?.let { oauthCallbackResult ->
            binding.usernameEditText.setText(oauthCallbackResult.possibleUsername)
            binding.emailEditText.setText(oauthCallbackResult.oauth?.email)
        }
    }

    private fun signUp() {
        val username = binding.usernameEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
        val email = binding.emailEditText.text.toString()
        val dob = binding.dobEditText.text.toString()
        val countryCode = "US"
        val iid: String? = arguments?.let { SignUpFragmentArgs.fromBundle(it) }?.oauthCallbackResult?.oauth?.iid
        val agreedToTos = binding.agreeToLegalCheckbox.isChecked
        val account = SignUpAccount(username, password, email, dob, countryCode)
        val signUpBody = SignUpBody(account, iid, agreedToTos)
        launch {
            val response = accountsService.signUp(signUpBody).await()
            Timber.d("Sign up API call succeeded $response")
            val credentials = response.body()?.credentials
            when {
                response.isSuccessful && credentials != null -> onSuccess(credentials)
                else -> onError(response)
            }
        }
    }

    private fun onSuccess(credentials: CaffeineCredentials) {
        tokenStore.storeCredentials(credentials)
        val navController = findNavController()
        val navOptions = NavOptions.Builder().setPopUpTo(navController.graph.id, true).build()
        navController.navigate(R.id.lobby, null, navOptions)
    }

    private fun onError(response: Response<SignUpResult>) {
        val errorBody = response.errorBody() ?: return
        val error = gson.fromJson(errorBody.string(), ApiErrorResult::class.java)
        Timber.d("Error: $error")
        val errors = error.errors
        binding.formErrorTextView.text = listOfNotNull(errors._error, errors.username, errors.password)
                .joinToString("\n") { it.joinToString("\n") }
    }

}
