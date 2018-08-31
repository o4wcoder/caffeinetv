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
import kotlinx.android.synthetic.main.fragment_mfa_code.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import okhttp3.ResponseBody
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.*
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
            val args = MfaCodeFragmentArgs.fromBundle(arguments)
            val username = args.username
            val password = args.password
            launch(CommonPool) {
                val result = accountsService.submitMfaCode(MfaCodeBody(Account(username, password), MfaCode(mfa_code_edit_text.text.toString()))).await()
                when {
                    result.isSuccessful -> onSuccess(result.body()!!)
                    else -> onError(result.errorBody()!!)
                }
            }
        }
    }

    private fun onSuccess(result: SignInResult) {
        sharedPreferences.edit { putString("REFRESH_TOKEN", result.refreshToken) }
        val navController = Navigation.findNavController(view!!)
        navController.navigate(R.id.lobby)
    }

    private fun onError(errorBody: ResponseBody) {
        val error = gson.fromJson(errorBody.string(), ApiErrorResult::class.java)
        Timber.d("Error: $error")
        launch(UI) {
            error.errors._error?.joinToString("\n")?.let { form_error_text_view.text = it }
            error.errors.otp?.joinToString("\n")?.let { mfa_code_text_input_layout.error = it }
        }
    }

}
