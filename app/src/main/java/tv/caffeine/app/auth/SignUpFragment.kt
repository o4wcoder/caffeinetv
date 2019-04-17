package tv.caffeine.app.auth

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.safetynet.SafetyNet
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.analytics.Analytics
import tv.caffeine.app.analytics.AnalyticsEvent
import tv.caffeine.app.analytics.FirebaseEvent
import tv.caffeine.app.analytics.logEvent
import tv.caffeine.app.api.*
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.databinding.FragmentSignUpBinding
import tv.caffeine.app.settings.LegalDoc
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.convertLinks
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.util.showSnackbar
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


class SignUpFragment : CaffeineFragment(), DatePickerDialog.OnDateSetListener {

    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var gson: Gson
    @Inject lateinit var analytics: Analytics
    @Inject lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var binding: FragmentSignUpBinding
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val args by navArgs<SignUpFragmentArgs>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.signUpButton.setOnClickListener { signUpClicked() }
        binding.agreeToLegalTextView.apply {
            text = convertLinks(R.string.i_agree_to_legal, resources, ::legalDocLinkSpanFactory)
            movementMethod = LinkMovementMethod.getInstance()
        }
        binding.dobEditText.apply {
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) { setDate() }
            }
            setOnClickListener { setDate() }
            setOnKeyListener { _, _, _ -> true }
        }
        arguments?.let { arguments ->
            binding.usernameEditText.setText(args.possibleUsername)
            binding.emailEditText.setText(args.email)
        }
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
        binding.dobEditText.setText(displayText, TextView.BufferType.NORMAL)
        binding.dobEditText.tag = apiText
    }

    private fun signUpClicked() {
        val iid: String? = args.iid
        if (iid != null) return signUp(null, iid)
        val context = context ?: return
        SafetyNet.getClient(context)
                .verifyWithRecaptcha(getString(R.string.safetynet_app_key))
                .addOnSuccessListener { response ->
                    val token = response.tokenResult
                    if (token?.isNotEmpty() == true) {
                        signUp(token, iid)
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

    private fun signUp(token: String?, iid: String?) {
        clearErrors()
        val username = binding.usernameEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
        val email = binding.emailEditText.text.toString()
        val dob = binding.dobEditText.tag as? String ?: ""
        val countryCode = "US"
        val account = SignUpAccount(username, password, email, dob, countryCode)
        val signUpBody = SignUpBody(account, iid, true, token)
        // TODO: better error handling before calling the API
        launch {
            val result = accountsService.signUp(signUpBody).awaitAndParseErrors(gson)
            when(result) {
                is CaffeineResult.Success -> onSuccess(result.value.credentials)
                is CaffeineResult.Error -> onError(result.error)
                is CaffeineResult.Failure -> onFailure(result.throwable)
            }
        }
    }

    @UiThread
    private fun onSuccess(credentials: CaffeineCredentials) {
        analytics.trackEvent(AnalyticsEvent.NewRegistration(credentials.caid))
        firebaseAnalytics.logEvent(FirebaseEvent.SignUpSuccess)
        tokenStore.storeCredentials(credentials)
        val navController = findNavController()
        val navOptions = NavOptions.Builder().setPopUpTo(navController.graph.id, true).build()
        navController.safeNavigate(R.id.main_nav, null, navOptions)
    }

    private fun clearErrors() {
        binding.formErrorTextView.isInvisible = true
    }

    @UiThread
    private fun onError(error: ApiErrorResult) {
        Timber.d("Error: $error")
        val errorMessages= listOfNotNull(
                listOfNotNull(error.generalErrorsString, error.deniedErrorsString).joinToString("\n"),
                error.emailErrorsString,
                error.usernameErrorsString,
                error.passwordErrorsString,
                error.dobErrorsString,
                getString(R.string.sign_up_description))
        errorMessages.firstOrNull { it.isNotEmpty() }?.let {
            binding.formErrorTextView.text = it
            binding.formErrorTextView.isInvisible = false
        }
    }

    @UiThread
    private fun onFailure(t: Throwable) {
        Timber.e(t, "sign up failure")
        showSnackbar(R.string.sign_up_failure)
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
