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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.*
import tv.caffeine.app.databinding.FragmentMfaCodeBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.setOnActionGo
import javax.inject.Inject

class MfaCodeFragment : CaffeineFragment() {

    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var sharedPreferences: SharedPreferences
    @Inject lateinit var gson: Gson
    private lateinit var binding: FragmentMfaCodeBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentMfaCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.submitMfaCodeButton.setOnClickListener {
            submitMfaCode()
        }
        binding.mfaCodeEditText.setOnActionGo { submitMfaCode() }
    }

    private fun submitMfaCode() {
        val args = MfaCodeFragmentArgs.fromBundle(arguments)
        val username = args.username
        val password = args.password
        launch {
            val result = accountsService.submitMfaCode(MfaCodeBody(Account(username, password), MfaCode(binding.mfaCodeEditText.text.toString()))).await()
            withContext(Dispatchers.Main) {
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
        error.errors._error?.joinToString("\n")?.let { binding.formErrorTextView.text = it }
        error.errors.otp?.joinToString("\n")?.let { binding.mfaCodeTextInputLayout.error = it }
    }

}
