package tv.caffeine.app.auth


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.ForgotPasswordBody
import tv.caffeine.app.databinding.FragmentForgotBinding
import javax.inject.Inject

class ForgotFragment : DaggerFragment() {
    @Inject lateinit var accountsService: AccountsService

    private lateinit var binding: FragmentForgotBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentForgotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.sendEmailButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            accountsService.forgotPassword(ForgotPasswordBody(email)).enqueue(object: Callback<Void?> {
                override fun onFailure(call: Call<Void?>?, t: Throwable?) {
                    Timber.e(t, "Failed to handle forgot password")
                }

                override fun onResponse(call: Call<Void?>?, response: Response<Void?>?) {
                    Timber.d("Handled forgot password $response")
                }
            })
        }
    }


}
