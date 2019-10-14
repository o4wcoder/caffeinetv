package tv.caffeine.app.login

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.View
import android.widget.DatePicker
import androidx.annotation.UiThread
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.safetynet.SafetyNet
import com.google.firebase.analytics.FirebaseAnalytics
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.analytics.Analytics
import tv.caffeine.app.analytics.AnalyticsEvent
import tv.caffeine.app.analytics.FirebaseEvent
import tv.caffeine.app.analytics.logEvent
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.CaffeineCredentials
import tv.caffeine.app.api.deniedErrorsString
import tv.caffeine.app.api.dobErrorsString
import tv.caffeine.app.api.emailErrorsString
import tv.caffeine.app.api.generalErrorsString
import tv.caffeine.app.api.isBirtdateError
import tv.caffeine.app.api.isEmailError
import tv.caffeine.app.api.isPasswordError
import tv.caffeine.app.api.isUsernameError
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.passwordErrorsString
import tv.caffeine.app.api.usernameErrorsString
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.databinding.FragmentSignUpBinding
import tv.caffeine.app.settings.LegalDoc
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.stripUrlUnderline
import tv.caffeine.app.util.convertLinks
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.util.showSnackbar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class SignUpFragment @Inject constructor(
    private val tokenStore: TokenStore,
    private val analytics: Analytics,
    private val firebaseAnalytics: FirebaseAnalytics
) : CaffeineFragment(R.layout.fragment_sign_up), DatePickerDialog.OnDateSetListener {

    private lateinit var binding: FragmentSignUpBinding
    private val viewModel: SignUpViewModel by viewModels { viewModelFactory }
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val args by navArgs<SignUpFragmentArgs>()

    private val doUseArkose = false

    val arkoseViewModel: ArkoseViewModel by navGraphViewModels(R.id.login) { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentSignUpBinding.bind(view)
        binding.viewModel = viewModel
        binding.emailEditText.afterTextChanged { viewModel.email = it }
        binding.usernameEditText.afterTextChanged { viewModel.username = it }
        binding.passwordEditText.afterTextChanged { viewModel.password = it }
        binding.dobEditText.afterTextChanged { viewModel.birthdate = it }
        binding.signUpButton.setOnClickListener { signUpClicked() }
        binding.agreeToLegalTextView.apply {
            text = convertLinks(R.string.i_agree_to_legal, resources, ::legalDocLinkSpanFactory)
            movementMethod = LinkMovementMethod.getInstance()
            stripUrlUnderline()
        }
        // TODO: Should not get access to the internal EditText. Need to create a
        // TODO: focus change listener for the whole layout
        binding.dobEditText.layoutEditText.apply {
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    setDate()
                }
            }
            setOnClickListener { setDate() }
        }

        arguments?.let {
            args.possibleUsername?.let { binding.usernameEditText.text = it }
            args.email?.let { binding.emailEditText.text = it }
        }

        arkoseViewModel.arkoseToken.observe(viewLifecycleOwner, Observer { event ->
            event.getContentIfNotHandled()?.let { token -> processArkoseTokenResult(token) }
        })
    }

    private fun setDate() {
        val dateText = binding.dobEditText.tag as? String
        val calendar = if (dateText?.isNotBlank() == true) {
            try {
                Calendar.getInstance().also { it.time = apiDateFormat.parse(dateText) }
            } catch (e: Exception) {
                Timber.e("Error parsing the date of birth")
                Calendar.getInstance()
            }
        } else {
            Calendar.getInstance()
        }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        context?.let {
            val dialog = DatePickerDialog(it, android.R.style.Theme_Holo_Light_Dialog, this, year, month, dayOfMonth)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
            ContextCompat.getColor(it, android.R.color.transparent).let { color ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setBackgroundColor(color)
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setBackgroundColor(color)
            }
        }
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        val calendar = Calendar.getInstance().also { it.set(year, month, dayOfMonth) }
        val displayText = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(LocalDate.of(year, month + 1, dayOfMonth))
        val apiText = apiDateFormat.format(calendar.time)
        binding.dobEditText.text = displayText
        arkoseViewModel.signUpBdayApiDate = apiText
    }

    private fun signUpClicked() {
        val iid: String? = args.iid
        if (iid != null) return signUp(null, null, iid)
        val context = context ?: return

        if (doUseArkose) {
            findNavController().safeNavigate(SignUpFragmentDirections.actionSignUpFragmentToArkoseFragment())
        } else {
            SafetyNet.getClient(context)
                .verifyWithRecaptcha(getString(R.string.safetynet_app_key))
                .addOnSuccessListener { response ->
                    val token = response.tokenResult
                    if (token?.isNotEmpty() == true) {
                        signUp(token, null, iid)
                    }
                }
                .addOnFailureListener {
                    if (it is ApiException) {
                        val reason = CommonStatusCodes.getStatusCodeString(it.statusCode)
                        Timber.e(Exception("Failed to do reCaptcha. Reason: $reason", it))
                    } else {
                        Timber.e(it, "Failed to do reCaptcha")
                    }
                    showSnackbar(R.string.recaptcha_failed)
                }
        }
    }

    private fun processArkoseTokenResult(token: String) {
        signUp(null, token, null)
    }

    private fun signUp(recaptchaToken: String?, arkoseToken: String?, iid: String?) {
        clearErrors()
        val dob = arkoseViewModel.signUpBdayApiDate ?: ""
        // TODO: better error handling before calling the API
        viewModel.signIn(dob, recaptchaToken, arkoseToken, iid).observe(viewLifecycleOwner, Observer { event ->
            event.getContentIfNotHandled()?.let { result ->
                when (result) {
                    is CaffeineResult.Success -> onSuccess(result.value.credentials)
                    is CaffeineResult.Error -> onError(result.error)
                    is CaffeineResult.Failure -> onFailure(result.throwable)
                }
            }
        })
    }

    @UiThread
    private fun onSuccess(credentials: CaffeineCredentials) {
        analytics.trackEvent(AnalyticsEvent.NewRegistration(credentials.caid))
        firebaseAnalytics.logEvent(FirebaseEvent.SignUpSuccess)
        tokenStore.storeCredentials(credentials)
        findNavController().safeNavigate(SignUpFragmentDirections.actionSignUpFragmentToWelcomeFragment(viewModel.email))
    }

    private fun clearErrors() {
        binding.emailEditText.clearError()
        binding.usernameEditText.clearError()
        binding.passwordEditText.clearError()
        binding.dobEditText.clearError()
    }

    @UiThread
    private fun onError(error: ApiErrorResult) {
        Timber.d("Error: $error")
        val errorMessages = listOfNotNull(
            listOfNotNull(error.generalErrorsString, error.deniedErrorsString).joinToString("\n"),
            error.emailErrorsString,
            error.usernameErrorsString,
            error.passwordErrorsString,
            error.dobErrorsString,
            getString(R.string.sign_up_description)
        )
        errorMessages.firstOrNull { it.isNotEmpty() }?.let {
            when {
                error.isEmailError() -> binding.emailEditText.showError(it)
                error.isUsernameError() -> binding.usernameEditText.showError(it)
                error.isPasswordError() -> binding.passwordEditText.showError(it)
                error.isBirtdateError() -> binding.dobEditText.showError(it)
                else -> binding.formErrorTextView.text = it
            }
        }
    }

    @UiThread
    private fun onFailure(t: Throwable) {
        Timber.e(t, "sign up failure")
        binding.formErrorTextView.text = getString(R.string.sign_up_failure)
    }

    private fun legalDocLinkSpanFactory(url: String?) =
            LegalDocLinkSpan(url, findNavController(), resources)

    private class LegalDocLinkSpan(url: String?, val navController: NavController, resources: Resources) : URLSpan(url) {
        val legalDoc = LegalDoc.values().find { resources.getString(it.url) == url }

        override fun onClick(widget: View) {
            legalDoc?.let {
                navController.safeNavigate(SignUpFragmentDirections.actionSignUpFragmentToLegalDocsFragment(it))
            }
        }
    }
}
