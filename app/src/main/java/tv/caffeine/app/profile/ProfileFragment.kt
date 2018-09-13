package tv.caffeine.app.profile


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.squareup.picasso.Picasso
import dagger.android.support.DaggerFragment
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.databinding.FragmentProfileBinding
import tv.caffeine.app.session.FollowManager
import javax.inject.Inject

class ProfileFragment : DaggerFragment() {
    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var followManager: FollowManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = FragmentProfileBinding.inflate(inflater, container, false)
        binding.signOutButton.setOnClickListener {
            tokenStore.clear()
            findNavController().popBackStack(R.id.landingFragment, false)
            accountsService.signOut().enqueue(object: Callback<Unit?> {
                override fun onFailure(call: Call<Unit?>?, t: Throwable?) {
                    Timber.e(t, "Failed to sign out")
                }

                override fun onResponse(call: Call<Unit?>?, response: Response<Unit?>?) {
                    Timber.d("Signed out successfully $response")
                }
            })
        }
        binding.ossLicensesButton.setOnClickListener {
            startActivity(Intent(context, OssLicensesMenuActivity::class.java))
        }
        tokenStore.caid?.let {
            launch {
                val self = followManager.userDetails(it)
                launch(UI) {
                    binding.usernameTextView.text = self.username
                    binding.nameTextView.text = self.name
                    binding.numberFollowingTextView.text = self.followingCount.toString()
                    binding.numberOfFollowersTextView.text = self.followersCount.toString()
                    Picasso.get()
                            .load(self.avatarImageUrl)
                            .centerCrop()
                            .resizeDimen(R.dimen.profile_size, R.dimen.profile_size)
                            .placeholder(R.drawable.default_avatar)
                            .transform(CropCircleTransformation())
                            .into(binding.avatarImageView)
                    if (self.isVerified) {
                        binding.usernameTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.verified_large, 0)
                    } else {
                        binding.usernameTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                    }
                }
            }
        }
        return binding.root
    }

}
