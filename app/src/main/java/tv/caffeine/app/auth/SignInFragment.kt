package tv.caffeine.app.auth


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.fragment_sign_in.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tv.caffeine.app.R

class SignInFragment : Fragment() {

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
        val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        val gsonConverterFactory = GsonConverterFactory.create(gson)
        val retrofit = Retrofit.Builder()
                .baseUrl("https://api.caffeine.tv")
                .addConverterFactory(gsonConverterFactory)
                .build()
        val accounts = retrofit.create(Accounts::class.java)
        val signInBody = SignInBody(Account(username, password))
        accounts.signin(signInBody).enqueue(object: Callback<SignInResult?> {
            override fun onFailure(call: Call<SignInResult?>?, t: Throwable?) {
                Log.d("API: Auth", "Login failed")
            }

            override fun onResponse(call: Call<SignInResult?>?, response: Response<SignInResult?>?) {
                Log.d("API: Auth", "Login successful, ${response?.body()}")
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
