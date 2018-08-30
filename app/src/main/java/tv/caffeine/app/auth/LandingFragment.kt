package tv.caffeine.app.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_landing.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.R
import javax.inject.Inject

class LandingFragment : DaggerFragment() {
    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var tokenStore: TokenStore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_landing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        new_account_button.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.signUpFragment))
        sign_in_with_email_button.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.signInFragment))
        loginIfPossible(view)
    }

    private fun loginIfPossible(view: View) {
        val refreshToken = tokenStore.refreshToken ?: return
        val refreshTokenBody = RefreshTokenBody(refreshToken)
        accountsService.refreshToken(refreshTokenBody).enqueue(object : Callback<RefreshTokenResult?> {
            override fun onFailure(call: Call<RefreshTokenResult?>?, t: Throwable?) {
                Timber.e(t, "Failed to login automatically")
            }

            override fun onResponse(call: Call<RefreshTokenResult?>?, response: Response<RefreshTokenResult?>?) {
                Timber.d("Request succeeded response: $response, body: ${response?.body()}")
                val refreshTokenResult = response?.body() ?: return
                val accessToken = refreshTokenResult.credentials.accessToken
                val xCredential = refreshTokenResult.credentials.credential
                val navController = Navigation.findNavController(view)
                navController.navigate(R.id.lobby)
            }
        })
    }

}
