package tv.caffeine.app.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import dagger.android.support.DaggerFragment
import tv.caffeine.app.R
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.databinding.FragmentLandingBinding
import javax.inject.Inject

class LandingFragment : DaggerFragment() {
    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var tokenStore: TokenStore
    private lateinit var binding: FragmentLandingBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentLandingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.newAccountButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.signUpFragment))
        binding.signInWithEmailButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.signInFragment))
    }

}
