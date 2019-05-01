package tv.caffeine.app.lobby

import android.content.res.Resources
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.firebase.analytics.FirebaseAnalytics
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import tv.caffeine.app.R
import tv.caffeine.app.analytics.logScreen
import tv.caffeine.app.broadcast.BroadcastPlaceholderDialogFragment
import tv.caffeine.app.databinding.FragmentLobbySwipeBinding
import tv.caffeine.app.feature.Feature
import tv.caffeine.app.feature.FeatureConfig
import tv.caffeine.app.profile.MyProfileViewModel
import tv.caffeine.app.session.SessionCheckViewModel
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.ViewPagerColorOnPageChangeListener
import tv.caffeine.app.util.maybeShow
import tv.caffeine.app.util.safeNavigate
import javax.inject.Inject

class LobbySwipeFragment : CaffeineFragment(R.layout.fragment_lobby_swipe) {

    @Inject lateinit var featureConfig: FeatureConfig
    @Inject lateinit var picasso: Picasso
    @Inject lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var binding: FragmentLobbySwipeBinding
    private val sessionCheckViewModel: SessionCheckViewModel by viewModels { viewModelFactory }
    private val myProfileViewModel: MyProfileViewModel by viewModels { viewModelFactory }
    private val viewPagerBackgroundColors = listOf(R.color.white, R.color.very_very_light_gray)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionCheckViewModel.sessionCheck.observe(this, Observer { result ->
            handle(result) {}
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        firebaseAnalytics.logScreen(this)
        FragmentLobbySwipeBinding.bind(view).apply {
            binding = this
            configure(this)
        }
    }

    override fun onResume() {
        super.onResume()
        // Set the correct background color in case no scroll is involved, e.g., from a deep link.
        binding.lobbyViewPager.apply {
            setBackgroundResource(viewPagerBackgroundColors.getOrElse(currentItem) { viewPagerBackgroundColors[0] })
        }
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
                    viewPagerBackgroundColors.map {
                        ContextCompat.getColor(context, it)
                    }))
        }

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

    override fun getCount() = 2
    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> LobbyFragment()
            else -> FeaturedProgramGuideFragment()
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> resources.getString(R.string.lobby_page_title_live_now)
            else -> resources.getString(R.string.lobby_page_title_upcoming)
        }
    }
}
