package tv.caffeine.app.auth

import android.app.DatePickerDialog
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.safetynet.SafetyNet
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.*
import tv.caffeine.app.databinding.FragmentSignUpBinding
import tv.caffeine.app.settings.LegalDoc
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.showSnackbar
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


class SignUpFragment : CaffeineFragment(), DatePickerDialog.OnDateSetListener {

    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var gson: Gson

    private lateinit var binding: FragmentSignUpBinding
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.signUpButton.setOnClickListener { signUpClicked() }
        binding.agreeToLegalTextView.apply {
            text = buildLegalDocSpannable(findNavController())
            movementMethod = LinkMovementMethod.getInstance()
        }
        binding.dobEditText.apply {
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) { setDate() }
            }
            setOnClickListener { setDate() }
            setOnKeyListener { _, _, _ -> true }
        }
        arguments?.let { SignUpFragmentArgs.fromBundle(it) }?.oauthCallbackResult?.let { oauthCallbackResult ->
            binding.usernameEditText.setText(oauthCallbackResult.possibleUsername)
            binding.emailEditText.setText(oauthCallbackResult.oauth?.email)
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
            DatePickerDialog(it, android.R.style.Theme_Holo_Light_Dialog, this, year, month, dayOfMonth).apply {
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                show()
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
        val iid: String? = arguments?.let { SignUpFragmentArgs.fromBundle(it) }?.oauthCallbackResult?.oauth?.iid
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
        val dob = binding.dobEditText.tag as String
        val countryCode = "US"
        val agreedToTos = binding.agreeToLegalCheckbox.isChecked
        val account = SignUpAccount(username, password, email, dob, countryCode)
        val signUpBody = SignUpBody(account, iid, agreedToTos, token)
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

    private fun clearErrors() {
        binding.formErrorTextView.text = null
        binding.usernameTextInputLayout.error = null
        binding.passwordTextInputLayout.error = null
        binding.emailTextInputLayout.error = null
        binding.dobTextInputLayout.error = null
    }

    private fun onError(response: Response<SignUpResult>) {
        val errorBody = response.errorBody() ?: return
        val error = gson.fromJson(errorBody.string(), ApiErrorResult::class.java)
        Timber.d("Error: $error")
        val errors = error.errors
        binding.formErrorTextView.text = listOfNotNull(errors._error, errors._denied)
                .joinToString("\n") { it.joinToString("\n") }
        binding.usernameTextInputLayout.error = errors.username?.joinToString("\n")
        binding.passwordTextInputLayout.error = errors.password?.joinToString("\n")
        binding.emailTextInputLayout.error = errors.email?.joinToString("\n")
        binding.dobTextInputLayout.error = errors.dob?.joinToString("\n")
    }

    private fun buildLegalDocSpannable(navController: NavController) : Spannable {
        val spannable = SpannableString(HtmlCompat.fromHtml(
                resources.getString(R.string.i_agree_to_legal), HtmlCompat.FROM_HTML_MODE_LEGACY))
        for (urlSpan in spannable.getSpans<URLSpan>(0, spannable.length, URLSpan::class.java)) {
            spannable.setSpan(LegalDocLinkSpan(urlSpan.url, navController, resources),
                    spannable.getSpanStart(urlSpan),
                    spannable.getSpanEnd(urlSpan),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.removeSpan(urlSpan)
        }
        return spannable
    }

    private class LegalDocLinkSpan(url: String?, val navController: NavController, resources: Resources) : URLSpan(url) {
        val legalDoc = LegalDoc.values().find { resources.getString(it.url) == url }

        override fun onClick(widget: View) {
            legalDoc?.let {
                navController.navigate(SignUpFragmentDirections.actionSignUpFragmentToLegalDocsFragment(it))
            }
        }
    }
}
