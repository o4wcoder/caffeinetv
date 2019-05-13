package tv.caffeine.app.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.ReasonKey
import tv.caffeine.app.api.ReportUserBody
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import tv.caffeine.app.ui.CaffeineDialogFragment
import tv.caffeine.app.ui.DialogActionBar
import tv.caffeine.app.util.showSnackbar
import javax.inject.Inject

class ReportDialogFragment : CaffeineDialogFragment() {

    private val viewModel: ReportUserViewModel by viewModels { viewModelFactory }
    private lateinit var caid: CAID
    private lateinit var username: String
    private var shouldNavigateBackWhenDone = false
    private val args by navArgs<ReportDialogFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullscreenDialogTheme)
        caid = args.caid
        username = args.username
        shouldNavigateBackWhenDone = args.shouldNavigateBackWhenDone
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_report_dialog, container, false)
        val reasonRadioGroup = view.findViewById<RadioGroup>(R.id.reason_radio_group)
        val descriptionEditText = view.findViewById<EditText>(R.id.description_edit_text)
        view.findViewById<DialogActionBar>(R.id.action_bar).apply {
            setTitle(getString(R.string.report_user, username))
            setActionText(R.string.submit)
            setActionListener { viewModel.reportUser(caid, getReason(reasonRadioGroup), descriptionEditText.text.toString()) }
            setDismissListener { dismiss() }
        }
        view.findViewById<TextView>(R.id.report_description).text = getString(R.string.report_description, username)

        viewModel.reportUserResult.observe(this, Observer { isSuccessful ->
            activity?.showSnackbar(if (isSuccessful) R.string.user_reported else R.string.failed_to_report_the_user)
            dismiss()
            if (shouldNavigateBackWhenDone) {
                findNavController().popBackStack()
            }
        })
        return view
    }

    private fun getReason(radioGroup: RadioGroup): ReasonKey {
        return when (radioGroup.checkedRadioButtonId) {
            R.id.harass_radio_button -> ReasonKey.HARASSMENT_OR_TROLLING
            R.id.inappropriate_radio_button -> ReasonKey.INAPPROPRIATE_CONTENT
            R.id.violence_radio_button -> ReasonKey.VIOLENCE_OR_SELF_HARM
            R.id.spam_radio_button -> ReasonKey.SPAM
            R.id.other_radio_button -> ReasonKey.OTHER
            else -> ReasonKey.OTHER
        }
    }
}

class ReportUserViewModel @Inject constructor(
    private val usersService: UsersService,
    private val gson: Gson
) : ViewModel() {
    private val _reportUserResult = MutableLiveData<Boolean>()
    val reportUserResult: LiveData<Boolean> = _reportUserResult.map { it }

    fun reportUser(caid: CAID, reason: ReasonKey, description: String?) {
        viewModelScope.launch {
            val result = usersService.report(caid, ReportUserBody(reason.name, description))
                    .awaitEmptyAndParseErrors(gson)
            when (result) {
                is CaffeineEmptyResult.Success -> _reportUserResult.value = true
                is CaffeineEmptyResult.Error -> _reportUserResult.value = false
                is CaffeineEmptyResult.Failure -> {
                    Timber.e(result.throwable, "Failed to report the user")
                    _reportUserResult.value = false
                }
            }
        }
    }
}
