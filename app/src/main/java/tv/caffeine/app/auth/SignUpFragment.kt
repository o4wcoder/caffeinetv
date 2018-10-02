package tv.caffeine.app.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import dagger.android.support.DaggerFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.SignUpAccount
import tv.caffeine.app.api.SignUpBody
import tv.caffeine.app.api.SignUpResult
import tv.caffeine.app.databinding.FragmentSignUpBinding
import javax.inject.Inject


class SignUpFragment : DaggerFragment() {

    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var tokenStore: TokenStore
    private lateinit var binding: tv.caffeine.app.databinding.FragmentSignUpBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.signUpButton.setOnClickListener { signUp() }
    }

    private fun signUp() {
        val username = binding.usernameEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
        val email = binding.emailEditText.text.toString()
        val dob = binding.dobEditText.text.toString()
        val countryCode = "US"
        val iid: String? = null
        val agreedToTos = binding.agreeToLegalCheckbox.isChecked
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
