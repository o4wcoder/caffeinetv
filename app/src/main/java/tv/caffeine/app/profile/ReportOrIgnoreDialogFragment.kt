package tv.caffeine.app.profile

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.core.text.HtmlCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.ui.CaffeineDialogFragment
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.util.showSnackbar
import javax.inject.Inject

class ReportOrIgnoreDialogFragment : CaffeineDialogFragment() {

    @Inject lateinit var usersService: UsersService
    private val viewModel by lazy { viewModelProvider.get(IgnoreUserViewModel::class.java) }
    private var shouldNavigateBackWhenDone = false

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

        val args=  ReportOrIgnoreDialogFragmentArgs.fromBundle(arguments)
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
            when(position) {
                0 -> reportUser(caid, username)
                1 -> viewModel.ignoreUser(caid)
                2 -> dismiss()
            }
        }
        return alert
    }

    private fun reportUser(caid: String, username: String) {
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

class IgnoreUserViewModel(
        dispatchConfig: DispatchConfig,
        private val tokenStore: TokenStore,
        private val usersService: UsersService,
        private val gson: Gson
): CaffeineViewModel(dispatchConfig) {
    private val _ignoreUserResult = MutableLiveData<Boolean>()
    val ignoreUserResult: LiveData<Boolean> = Transformations.map(_ignoreUserResult) { it }

    fun ignoreUser(ignoree: String) {
        val ignorer= tokenStore.caid ?: return
        launch {
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
