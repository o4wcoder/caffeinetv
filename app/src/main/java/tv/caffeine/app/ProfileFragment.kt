package tv.caffeine.app


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_profile.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.auth.TokenStore
import javax.inject.Inject

class ProfileFragment : DaggerFragment() {
    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var tokenStore: TokenStore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sign_out_button.setOnClickListener {
            tokenStore.clear()
            findNavController().navigateUp()
            accountsService.signOut().enqueue(object: Callback<Unit?> {
                override fun onFailure(call: Call<Unit?>?, t: Throwable?) {
                    Timber.e(t, "Failed to sign out")
                }

                override fun onResponse(call: Call<Unit?>?, response: Response<Unit?>?) {
                    Timber.d("Signed out successfully $response")
                }
            })
        }
        oss_licenses_button.setOnClickListener {
            startActivity(Intent(context, OssLicensesMenuActivity::class.java))
        }
    }

}
