package tv.caffeine.app.auth


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_sign_in.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.R
import javax.inject.Inject

class SignInFragment : DaggerFragment() {
    @Inject lateinit var accounts: Accounts

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sign_in, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        forgot_button.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.action_signInFragment_to_forgotFragment))
        sign_in_button.setOnClickListener {
            login(username_edit_text.text.toString(), password_edit_text.text.toString())
        }
    }

    private fun login(username: String, password: String) {
        val signInBody = SignInBody(Account(username, password))
        accounts.signin(signInBody).enqueue(object: Callback<SignInResult?> {
            override fun onFailure(call: Call<SignInResult?>?, t: Throwable?) {
                Timber.d("Login failed")
            }

            override fun onResponse(call: Call<SignInResult?>?, response: Response<SignInResult?>?) {
                Timber.d("Login successful, ${response?.body()}")
                response?.body()?.refreshToken?.let { refreshToken ->
                    activity?.getSharedPreferences("caffeine", Context.MODE_PRIVATE)?.edit()?.putString("REFRESH_TOKEN", refreshToken)?.apply()
                }
                response?.body()?.accessToken?.run {
                    val bundle = Bundle()
                    bundle.putString("ACCESS_TOKEN", this)
                    val navController = Navigation.findNavController(view!!)
                    val navOptions = NavOptions.Builder().setPopUpTo(navController.graph.id, true).build()
                    navController.navigate(R.id.action_signInFragment_to_lobby, bundle, navOptions)
                }
            }
        })
    }
}
