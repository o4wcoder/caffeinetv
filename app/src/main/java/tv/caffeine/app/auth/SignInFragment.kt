package tv.caffeine.app.auth


import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.navigation.Navigation
import com.google.gson.Gson
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_sign_in.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import okhttp3.ResponseBody
import timber.log.Timber
import tv.caffeine.app.R
import javax.inject.Inject

class SignInFragment : DaggerFragment() {
    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var sharedPreferences: SharedPreferences
    @Inject lateinit var gson: Gson

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
        sharedPreferences.edit { putString("REFRESH_TOKEN", signInResult.refreshToken) }
        val navController = Navigation.findNavController(view!!)
        when(signInResult.next) {
            "mfa_otp_required" -> {
                val username = username_edit_text.text.toString()
                val password = password_edit_text.text.toString()
                val action = SignInFragmentDirections.actionSignInFragmentToMfaCodeFragment(username, password)
                navController.navigate(action)
            }
            else -> {
                val bundle = Bundle()
                bundle.putString("ACCESS_TOKEN", signInResult.accessToken)
                bundle.putString("X_CREDENTIAL", signInResult.credentials.credential)
                navController.navigate(R.id.lobby, bundle)
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
