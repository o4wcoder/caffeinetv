package tv.caffeine.app.profile

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.core.text.HtmlCompat
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
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.ui.CaffeineDialogFragment
import tv.caffeine.app.util.showSnackbar
import javax.inject.Inject

class ReportOrIgnoreDialogFragment : CaffeineDialogFragment() {

    private val viewModel: IgnoreUserViewModel by viewModels { viewModelFactory }
    private var shouldNavigateBackWhenDone = false
    private val args by navArgs<ReportOrIgnoreDialogFragmentArgs>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (activity == null) {
            IllegalStateException("Activity cannot be null").let {
                Timber.e(it)
                throw it
            }
        }
        viewModel.ignoreUserResult.observe(this, Observer { isSuccessful ->
            activity?.showSnackbar(if (isSuccessful) R.string.user_ignored else R.string.failed_to_ignore_the_user)
            dismiss()
            if (shouldNavigateBackWhenDone) {
                findNavController().popBackStack()
            }
        })

        val caid = args.caid
        val username = args.username
        shouldNavigateBackWhenDone = args.shouldNavigateBackWhenDone
        val text = arrayOf(
                HtmlCompat.fromHtml(getString(R.string.report_user_more_dialog_option, username), HtmlCompat.FROM_HTML_MODE_LEGACY),
                HtmlCompat.fromHtml(getString(R.string.ignore_user_dialog_option, username), HtmlCompat.FROM_HTML_MODE_LEGACY),
                getString(R.string.cancel)
        )
        val alert = AlertDialog.Builder(activity)
                .setItems(text, null)
                .create()
        alert.listView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> reportUser(caid, username)
                1 -> viewModel.ignoreUser(caid)
                2 -> dismiss()
            }
        }
        return alert
    }

    private fun reportUser(caid: CAID, username: String) {
        dismiss()
        fragmentManager?.let { fm ->
            ReportDialogFragment().apply {
                arguments = ReportOrIgnoreDialogFragmentDirections
                        .actionProfileFragmentToReportDialogFragment(caid, username, shouldNavigateBackWhenDone).arguments
                show(fm, "reportUser")
            }
        }
    }
}

class IgnoreUserViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val usersService: UsersService,
    private val gson: Gson
) : ViewModel() {
    private val _ignoreUserResult = MutableLiveData<Boolean>()
    val ignoreUserResult: LiveData<Boolean> = _ignoreUserResult.map { it }

    fun ignoreUser(ignoree: String) {
        val ignorer = tokenStore.caid ?: return
        viewModelScope.launch {
            val result = usersService.ignore(ignorer, ignoree).awaitEmptyAndParseErrors(gson)
            when (result) {
                is CaffeineEmptyResult.Success -> _ignoreUserResult.value = true
                is CaffeineEmptyResult.Error -> _ignoreUserResult.value = false
                is CaffeineEmptyResult.Failure -> {
                    Timber.e(result.throwable, "Failed to ignore the user")
                    _ignoreUserResult.value = false
                }
            }
        }
    }
}
