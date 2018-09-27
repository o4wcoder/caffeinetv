package tv.caffeine.app.auth


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_sign_in.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.android.Main
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.*
import tv.caffeine.app.ui.setOnActionGo
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
            login()
        }
        password_edit_text.setOnActionGo { login() }
    }

    private fun login() {
        val username = username_edit_text.text.toString()
        val password = password_edit_text.text.toString()
        form_error_text_view.text = null
        val signInBody = SignInBody(Account(username, password))
        GlobalScope.launch(Dispatchers.Default) {
            val request = accountsService.signIn(signInBody).await()
            launch(Dispatchers.Main) {
                when {
                    request.isSuccessful -> onSuccess(request.body()!!)
                    else -> onError(request.errorBody()!!)
                }
            }
        }
    }

    @UiThread
    private fun onSuccess(signInResult: SignInResult) {
        val navController = findNavController()
        when(signInResult.next) {
            "mfa_otp_required" -> {
                val username = username_edit_text.text.toString()
                val password = password_edit_text.text.toString()
                val action = SignInFragmentDirections.actionSignInFragmentToMfaCodeFragment(username, password)
                navController.navigate(action)
            }
            else -> {
                tokenStore.storeSignInResult(signInResult)
                navController.popBackStack(R.id.landingFragment, true)
                navController.navigate(R.id.lobbyFragment)
            }
        }
    }

    @UiThread
    private fun onError(signInError: ResponseBody) {
        val error = gson.fromJson(signInError.string(), ApiErrorResult::class.java)
        Timber.d("Error: $error")
        error.errors._error?.joinToString("\n")?.let { form_error_text_view.text = it }
        error.errors.username?.joinToString("\n")?.let { username_text_input_layout.error = it }
        error.errors.password?.joinToString("\n")?.let { password_text_input_layout.error = it }
    }
}
