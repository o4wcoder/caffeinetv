package tv.caffeine.app.auth

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.fragment_landing.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tv.caffeine.app.R

class LandingFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_landing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        new_account_button.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.action_landingFragment_to_signUpFragment))
        sign_in_with_email_button.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.action_landingFragment_to_signInFragment))
        activity?.let { activity ->
            activity.getSharedPreferences("caffeine", Context.MODE_PRIVATE).let {
                it.getString("REFRESH_TOKEN", null)?.let { refreshToken ->
                    val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
                    val gsonConverterFactory = GsonConverterFactory.create(gson)
                    val retrofit = Retrofit.Builder()
                            .baseUrl("https://api.caffeine.tv")
                            .addConverterFactory(gsonConverterFactory)
                            .build()
                    val accounts = retrofit.create(Accounts::class.java)
                    val refreshTokenBody = RefreshTokenBody(refreshToken)
                    accounts.refreshToken(refreshTokenBody).enqueue(object: Callback<RefreshTokenResult?> {
                        override fun onFailure(call: Call<RefreshTokenResult?>?, t: Throwable?) {
                            Log.e("AutoLogin", "Failed to login", t)
                        }

                        override fun onResponse(call: Call<RefreshTokenResult?>?, response: Response<RefreshTokenResult?>?) {
                            Log.d("AutoLogin", "Request succeeded")
                            response?.body()?.credentials?.accessToken?.run {
                                val bundle = Bundle()
                                bundle.putString("ACCESS_TOKEN", this)
                                val navController = Navigation.findNavController(view)
                                navController.navigate(R.id.action_landingFragment_to_lobby, bundle)
                            }
                        }
                    })
                }
            }
        }
    }

}
