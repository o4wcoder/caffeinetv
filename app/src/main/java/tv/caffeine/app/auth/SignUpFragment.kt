package tv.caffeine.app.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_sign_up.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.SignUpAccount
import tv.caffeine.app.api.SignUpBody
import tv.caffeine.app.api.SignUpResult
import javax.inject.Inject


class SignUpFragment : DaggerFragment() {

    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var tokenStore: TokenStore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sign_up_button.setOnClickListener { signUp() }
    }

    private fun signUp() {
        val username = username_edit_text.text.toString()
        val password = password_edit_text.text.toString()
        val email = email_edit_text.text.toString()
        val dob = dob_edit_text.text.toString()
        val countryCode = "US"
        val iid: String? = null
        val agreedToTos = agree_to_legal_checkbox.isChecked
        val account = SignUpAccount(username, password, email, dob, countryCode)
        val signUpBody = SignUpBody(account, iid, agreedToTos)
        accountsService.signUp(signUpBody).enqueue(object : Callback<SignUpResult?> {
            override fun onFailure(call: Call<SignUpResult?>?, t: Throwable?) {
                Timber.e(t, "Failed to sign up")
            }

            override fun onResponse(call: Call<SignUpResult?>?, response: Response<SignUpResult?>?) {
                Timber.d("Sign up API call succeeded $response")
                response?.body()?.credentials?.let {
                    tokenStore.storeCredentials(it)
                    val navController = findNavController()
                    val navOptions = NavOptions.Builder().setPopUpTo(navController.graph.id, true).build()
                    navController.navigate(R.id.lobby, null, navOptions)
                }
            }
        })
    }

}
