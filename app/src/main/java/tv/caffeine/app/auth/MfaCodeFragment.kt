package tv.caffeine.app.auth


import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.core.content.edit
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_mfa_code.*
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.launch
import okhttp3.ResponseBody
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.*
import tv.caffeine.app.ui.setOnActionGo
import javax.inject.Inject

class MfaCodeFragment : DaggerFragment() {

    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var sharedPreferences: SharedPreferences
    @Inject lateinit var gson: Gson

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_mfa_code, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        submit_mfa_code_button.setOnClickListener {
            submitMfaCode()
        }
        mfa_code_edit_text.setOnActionGo { submitMfaCode() }
    }

    private fun submitMfaCode() {
        val args = MfaCodeFragmentArgs.fromBundle(arguments)
        val username = args.username
        val password = args.password
        GlobalScope.launch(Dispatchers.Default) {
            val result = accountsService.submitMfaCode(MfaCodeBody(Account(username, password), MfaCode(mfa_code_edit_text.text.toString()))).await()
            launch(Dispatchers.Main) {
                when {
                    result.isSuccessful -> onSuccess(result.body()!!)
                    else -> onError(result.errorBody()!!)
                }
            }
        }
    }

    @UiThread
    private fun onSuccess(result: SignInResult) {
        sharedPreferences.edit { putString("REFRESH_TOKEN", result.refreshToken) }
        val navController = findNavController()
        navController.popBackStack(R.id.landingFragment, true)
        navController.navigate(R.id.lobbyFragment)
    }

    @UiThread
    private fun onError(errorBody: ResponseBody) {
        val error = gson.fromJson(errorBody.string(), ApiErrorResult::class.java)
        Timber.d("Error: $error")
        error.errors._error?.joinToString("\n")?.let { form_error_text_view.text = it }
        error.errors.otp?.joinToString("\n")?.let { mfa_code_text_input_layout.error = it }
    }

}
