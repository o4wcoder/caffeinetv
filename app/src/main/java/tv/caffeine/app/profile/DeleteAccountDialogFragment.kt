package tv.caffeine.app.profile

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import android.widget.EditText
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
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.DeleteAccountBody
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.ui.CaffeineDialogFragment
import tv.caffeine.app.util.navigateToLanding
import javax.inject.Inject

class DeleteAccountDialogFragment : CaffeineDialogFragment() {

    private lateinit var username: String
    private lateinit var passwordEditText: EditText
    private val viewModel: DeleteAccountViewModel by viewModels { viewModelFactory }
    private val args by navArgs<DeleteAccountDialogFragmentArgs>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (activity == null) {
            IllegalStateException("Activity cannot be null").let {
                Timber.e(it)
                throw it
            }
        }
        username = args.username
        viewModel.deleteAccountResult.observe(this, Observer { result ->
            if (result.isSuccessful) {
                dismiss()
                findNavController().navigateToLanding(getString(R.string.account_deleted))
            } else {
                passwordEditText.error = getString(R.string.delete_account_incorrect_password_error)
            }
        })
        val alert = AlertDialog.Builder(activity)
                .setTitle(R.string.delete_account_question)
                .setMessage(resources.getString(R.string.delete_account_message, username))
                .setPositiveButton(R.string.delete_account_button, null)
                .setNegativeButton(R.string.cancel, null)
                .setView(R.layout.dialog_password)
                .create()
        alert.show()
        alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { deleteAccount() }
        alert.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener { dismiss() }
        alert.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        passwordEditText = alert.findViewById(R.id.password_edit_text)
        passwordEditText.requestFocus()
        return alert
    }

    private fun deleteAccount() {
        val password = passwordEditText.text.toString()
        if (password.isEmpty()) {
            passwordEditText.error = getString(R.string.delete_account_empty_password_error)
        } else {
            viewModel.deleteAccount(password)
        }
    }
}

class DeleteAccountViewModel @Inject constructor(
    private val accountsService: AccountsService,
    private val tokenStore: TokenStore,
    private val gson: Gson
) : ViewModel() {

    private val _deleteAccountResult = MutableLiveData<DeleteAccountResult>()
    val deleteAccountResult: LiveData<DeleteAccountResult> = _deleteAccountResult.map { it }

    fun deleteAccount(password: String) {
        val caid = tokenStore.caid ?: return
        viewModelScope.launch {
            val result = accountsService.deleteAccount(caid, DeleteAccountBody(DeleteAccountBody.Account(password)))
                    .awaitEmptyAndParseErrors(gson)
            when (result) {
                is CaffeineEmptyResult.Success -> {
                    _deleteAccountResult.value = DeleteAccountResult(true)
                    tokenStore.clear()
                }
                is CaffeineEmptyResult.Error -> _deleteAccountResult.value = DeleteAccountResult(false)
                is CaffeineEmptyResult.Failure -> Timber.e(result.throwable, "Failed to delete the account")
            }
        }
    }
}

class DeleteAccountResult(var isSuccessful: Boolean)
