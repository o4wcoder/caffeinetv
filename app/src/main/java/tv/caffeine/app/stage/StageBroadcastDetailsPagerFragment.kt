package tv.caffeine.app.stage

import android.content.res.Resources
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import org.threeten.bp.Clock
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentStageBroadcastDetailsPagerBinding
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.stage.biography.BiographyFragment
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.users.FollowersFragment
import tv.caffeine.app.users.FollowersFragmentArgs
import tv.caffeine.app.users.FollowingFragment
import tv.caffeine.app.users.FollowingFragmentArgs
import javax.inject.Inject
import javax.inject.Provider

private const val NUM_DETAILS_TABS = 3

class StageBroadcastDetailsPagerFragment @Inject constructor(
    private val adapterFactory: StageBroadcastDetailsPagerAdapter.Factory,
    private val followManager: FollowManager,
    private val clock: Clock
) : CaffeineFragment(R.layout.fragment_stage_broadcast_details_pager) {

    private lateinit var binding: FragmentStageBroadcastDetailsPagerBinding
    private val viewModel: StageBroadcastDetailsPagerViewModel by viewModels { viewModelFactory }
    private val args by navArgs<StageBroadcastDetailsPagerFragmentArgs>()

    interface Callback {
        fun returnToChat()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentStageBroadcastDetailsPagerBinding.bind(view)
        binding.viewModel = viewModel

        viewModel.loadUserProfile(args.broadcastUsername)
            .observe(viewLifecycleOwner, Observer { userProfile ->
                userProfile?.let {
                    binding.shareButton?.setOnClickListener {
                        val sharerId = followManager.currentUserDetails()?.caid
                        startActivity(
                            StageShareIntentBuilder(
                                userProfile,
                                sharerId,
                                resources,
                                clock
                            ).build()
                        )
                    }
                }
            })

        binding.stageBroadcastDetailsViewPager.adapter = adapterFactory.create(childFragmentManager, args.caid)
        binding.chatButton.setOnClickListener { (parentFragment as? StageFragment)?.returnToChat() }
    }
}

class StageBroadcastDetailsPagerAdapter @AssistedInject constructor(
    @Assisted fm: FragmentManager,
    @Assisted private val caid: String,
    private val resources: Resources,
    private val followingFragmentProvider: Provider<FollowingFragment>,
    private val followersFragmentProvider: Provider<FollowersFragment>
) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    @AssistedInject.Factory
    interface Factory {
        fun create(fm: FragmentManager, caid: String): StageBroadcastDetailsPagerAdapter
    }
    override fun getItem(position: Int): Fragment =
        when (position) {
            0 -> BiographyFragment.newInstance()
            1 -> { followingFragmentProvider.get().apply { arguments = FollowingFragmentArgs(caid).toBundle() } }
            2 -> { followersFragmentProvider.get().apply { arguments = FollowersFragmentArgs(caid).toBundle() } }
            else -> throw IllegalStateException("Unknown exception")
        }

    override fun getCount() = NUM_DETAILS_TABS

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> resources.getString(R.string.stage_broadcast_biography_tab)
            1 -> resources.getString(R.string.stage_broadcast_following_tab)
            else -> resources.getString(R.string.stage_broadcast_followers_tab)
        }
    }
}