package tv.caffeine.app.profile

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.DeleteAccountBody
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.di.ViewModelFactory
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.util.navigateToLanding
import java.lang.IllegalStateException
import javax.inject.Inject

class DeleteAccountDialogFragment : DialogFragment(), HasSupportFragmentInjector {

    @Inject lateinit var childFragmentInjector: DispatchingAndroidInjector<Fragment>
    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var viewModelFactory: ViewModelFactory

    private lateinit var username: String
    private lateinit var passwordEditText: EditText
    private val viewModelProvider by lazy { ViewModelProviders.of(this, viewModelFactory)}
    private val viewModel by lazy { viewModelProvider.get(DeleteAccountViewModel::class.java) }

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = childFragmentInjector

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (activity == null) {
            IllegalStateException("Activity cannot be null").let {
                Timber.e(it)
                throw it
            }
        }
        username = DeleteAccountDialogFragmentArgs.fromBundle(arguments).username
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

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
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

class DeleteAccountViewModel(
        dispatchConfig: DispatchConfig,
        private val accountsService: AccountsService,
        private val tokenStore: TokenStore,
        private val gson: Gson
): CaffeineViewModel(dispatchConfig) {

    private val _deleteAccountResult = MutableLiveData<DeleteAccountResult>()
    val deleteAccountResult: LiveData<DeleteAccountResult> = Transformations.map(_deleteAccountResult) { it }

    fun deleteAccount(password: String) {
        val caid = tokenStore.caid ?: return
        launch {
            val result = accountsService.deleteAccount(caid, DeleteAccountBody(DeleteAccountBody.Account(password))).awaitEmptyAndParseErrors(gson)
            when (result) {
                is CaffeineEmptyResult.Success-> {
                    _deleteAccountResult.value = DeleteAccountResult(true)
                    tokenStore.clear()
                }
                is CaffeineEmptyResult.Error -> _deleteAccountResult.value = DeleteAccountResult(false)
                is CaffeineEmptyResult.Failure -> Timber.e(result.exception, "Failed to delete the account")
            }
        }
    }
}

class DeleteAccountResult(var isSuccessful: Boolean)
