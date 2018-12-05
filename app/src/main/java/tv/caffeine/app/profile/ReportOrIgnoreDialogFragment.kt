package tv.caffeine.app.profile

import tv.caffeine.app.R
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.BooleanResult
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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (activity == null) {
            IllegalStateException("Activity cannot be null").let {
                Timber.e(it)
                throw it
            }
        }
        viewModel.ignoreUserResult.observe(this, Observer {
            activity?.showSnackbar(if (it.isSuccessful) R.string.user_ignored else R.string.failed_to_ignore_the_user)
            dismiss()
            // TODO (david) different navigation backstack behaviors depending on where the dialog is opened.
        })
        val args=  ReportOrIgnoreDialogFragmentArgs.fromBundle(arguments)
        val caid = args.caid
        val username = args.username
        val text = arrayOf(
                getString(R.string.report_user_more, username),
                getString(R.string.ignore_user, username)
        )
        val alert = AlertDialog.Builder(activity)
                .setItems(text, null)
                .create()
        alert.listView.setOnItemClickListener { parent, view, position, id ->
            when(position) {
                0 -> reportUser(caid, username)
                1 -> viewModel.ignoreUser(caid)
            }
        }
        return alert
    }

    private fun reportUser(caid: String, username: String) {
        fragmentManager?.let { fm ->
            ReportDialogFragment().apply {
                arguments = ReportOrIgnoreDialogFragmentDirections
                        .actionProfileFragmentToReportDialogFragment(caid, username).arguments
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
    private val _ignoreUserResult = MutableLiveData<BooleanResult>()
    val ignoreUserResult: LiveData<BooleanResult> = Transformations.map(_ignoreUserResult) { it }

    fun ignoreUser(ignoree: String) {
        val ignorer= tokenStore.caid ?: return
        launch {
            val result = usersService.ignore(ignorer, ignoree).awaitEmptyAndParseErrors(gson)
            when (result) {
                is CaffeineEmptyResult.Success -> _ignoreUserResult.value = BooleanResult(true)
                is CaffeineEmptyResult.Error -> _ignoreUserResult.value = BooleanResult(false)
                is CaffeineEmptyResult.Failure -> {
                    Timber.e(result.exception, "Failed to ignore the user")
                    _ignoreUserResult.value = BooleanResult(false)
                }
            }
        }
    }
}