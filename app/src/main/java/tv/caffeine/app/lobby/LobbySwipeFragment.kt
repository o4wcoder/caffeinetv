package tv.caffeine.app.lobby

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import tv.caffeine.app.BuildConfig
import tv.caffeine.app.R
import tv.caffeine.app.broadcast.BroadcastPlaceholderDialogFragment
import tv.caffeine.app.databinding.FragmentLobbySwipeBinding
import tv.caffeine.app.feature.Feature
import tv.caffeine.app.feature.FeatureConfig
import tv.caffeine.app.profile.MyProfileViewModel
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.ViewPagerColorOnPageChangeListener
import tv.caffeine.app.util.maybeShow
import tv.caffeine.app.util.safeNavigate
import javax.inject.Inject

private const val IS_FPG_ENABLED = BuildConfig.FLAVOR == "staging"

class LobbySwipeFragment : CaffeineFragment() {

    @Inject lateinit var featureConfig: FeatureConfig
    @Inject lateinit var picasso: Picasso

    private val myProfileViewModel: MyProfileViewModel by viewModels { viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentLobbySwipeBinding.inflate(inflater, container, false)
        configure(binding)
        return binding.root
    }

    private fun configure(binding: FragmentLobbySwipeBinding) {
        binding.lifecycleOwner = viewLifecycleOwner

        binding.cameraButton.setOnClickListener {
            if (featureConfig.isFeatureEnabled(Feature.BROADCAST)) {
                val action = LobbySwipeFragmentDirections.actionLobbySwipeFragmentToBroadcastFragment()
                findNavController().safeNavigate(action)
            } else {
                BroadcastPlaceholderDialogFragment().maybeShow(fragmentManager, "broadcastPlaceholder")
            }
        }
        binding.searchButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.exploreFragment))
        binding.activityButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.notificationsFragment))
        binding.profileButton.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.myProfileFragment))

        binding.lobbyViewPager.apply {
            adapter = LobbyPagerAdapter(childFragmentManager, resources)
            addOnPageChangeListener(ViewPagerColorOnPageChangeListener(this,
                    listOf(R.color.white, R.color.light_gray).map {
                        ContextCompat.getColor(context, it)
                    }))
        }
        binding.tabLayout.isVisible = IS_FPG_ENABLED

        myProfileViewModel.userProfile.observe(viewLifecycleOwner, Observer { userProfile ->
            picasso
                    .load(userProfile.avatarImageUrl)
                    .resizeDimen(R.dimen.toolbar_icon_size, R.dimen.toolbar_icon_size)
                    .transform(CropCircleTransformation())
                    .into(binding.profileButton)
            binding.unverifiedMessageTextView.isVisible = userProfile.emailVerified == false
        })
    }
}

class LobbyPagerAdapter(fm: FragmentManager, val resources: Resources) : FragmentStatePagerAdapter(fm) {

    override fun getCount() = if (IS_FPG_ENABLED) 2 else 1
    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> LobbyFragment()
            else -> FeaturedProgramGuideFragment()
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> resources.getString(R.string.lobby_page_title_watch_now)
            else -> resources.getString(R.string.lobby_page_title_scheduled)
        }
    }
}
