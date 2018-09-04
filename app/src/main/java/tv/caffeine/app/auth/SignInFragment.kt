package tv.caffeine.app.auth


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_sign_in.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import okhttp3.ResponseBody
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.*
import javax.inject.Inject

class SignInFragment : DaggerFragment() {
    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var gson: Gson
    @Inject lateinit var tokenStore: TokenStore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sign_in, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        forgot_button.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.forgotFragment))
        sign_in_button.setOnClickListener {
            login(username_edit_text.text.toString(), password_edit_text.text.toString())
        }
    }

    private fun login(username: String, password: String) {
        form_error_text_view.text = null
        val signInBody = SignInBody(Account(username, password))
        launch(CommonPool) {
            val request = accountsService.signIn(signInBody).await()
            when {
                request.isSuccessful -> onSuccess(request.body()!!)
                else -> onError(request.errorBody()!!)
            }
        }
    }

    private fun onSuccess(signInResult: SignInResult) {
        tokenStore.storeSignInResult(signInResult)
        val navController = findNavController()
        when(signInResult.next) {
            "mfa_otp_required" -> {
                val username = username_edit_text.text.toString()
                val password = password_edit_text.text.toString()
                val action = SignInFragmentDirections.actionSignInFragmentToMfaCodeFragment(username, password)
                navController.navigate(action)
            }
            else -> {
                navController.navigate(R.id.lobby)
            }
        }
    }

    private fun onError(signInError: ResponseBody) {
        val error = gson.fromJson(signInError.string(), ApiErrorResult::class.java)
        Timber.d("Error: $error")
        launch(UI) {
            error.errors._error?.joinToString("\n")?.let { form_error_text_view.text = it }
            error.errors.username?.joinToString("\n")?.let { username_text_input_layout.error = it }
            error.errors.password?.joinToString("\n")?.let { password_text_input_layout.error = it }
        }
    }
}
