package tv.caffeine.app.stage

import android.content.res.Resources
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.navigation.fragment.navArgs
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import tv.caffeine.app.R
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.databinding.FragmentStageBroadcastProfilePagerBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.users.FollowersFragment
import tv.caffeine.app.users.FollowersFragmentArgs
import tv.caffeine.app.users.FollowingFragment
import tv.caffeine.app.users.FollowingFragmentArgs
import javax.inject.Inject
import javax.inject.Provider

private const val NUM_DETAILS_TABS = 3

class StageBroadcastProfilePagerFragment @Inject constructor(
    private val adapterFactory: StageBroadcastProfilePagerAdapter.Factory
) : CaffeineFragment(R.layout.fragment_stage_broadcast_profile_pager) {

    private lateinit var binding: FragmentStageBroadcastProfilePagerBinding
    private val args by navArgs<StageBroadcastProfilePagerFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentStageBroadcastProfilePagerBinding.bind(view)
        binding.stageBroadcastDetailsViewPager.adapter = adapterFactory.create(childFragmentManager, args.caid, args.broadcastUsername)
        binding.giftButton.setOnClickListener { processChatAction(ChatAction.DIGITAL_ITEM) }
        binding.reactButton.setOnClickListener { processChatAction(ChatAction.MESSAGE) }
        binding.shareButton.setOnClickListener { processChatAction(ChatAction.SHARE) }
        binding.tabLayout.getTabAt(1)?.text = getFollowersTabTitle()
        binding.tabLayout.getTabAt(2)?.text = getFollowingTabTitle()
    }

    private fun processChatAction(chatAction: ChatAction) {
        (parentFragment as? StageFragment)?.processChatAction(chatAction)
    }

    fun getFollowingTabTitle() =
        args.followingCountString?.let { getString(R.string.numbered_stage_broadcast_following_tab, it) }
            ?: getString(R.string.stage_broadcast_following_tab)

    fun getFollowersTabTitle() =
        args.followersCountString?.let {
            val followersCount = if (it.toIntOrNull() == 1) 1 else 2
            resources.getQuantityString(R.plurals.numbered_stage_broadcast_followers_tab, followersCount, it)
        }
            ?: getString(R.string.stage_broadcast_followers_tab)
}

class StageBroadcastProfilePagerAdapter @AssistedInject constructor(
    @Assisted fm: FragmentManager,
    @Assisted private val caid: CAID,
    @Assisted private val username: String,
    private val resources: Resources,
    private val aboutFragmentProvider: Provider<AboutFragment>,
    private val followingFragmentProvider: Provider<FollowingFragment>,
    private val followersFragmentProvider: Provider<FollowersFragment>
) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    @AssistedInject.Factory
    interface Factory {
        fun create(fm: FragmentManager, caid: CAID, username: String): StageBroadcastProfilePagerAdapter
    }
    override fun getItem(position: Int): Fragment =
        when (position) {
            0 -> { aboutFragmentProvider.get().apply { arguments = AboutFragmentArgs(caid).toBundle() } }
            1 -> { followersFragmentProvider.get().apply { arguments = FollowersFragmentArgs(caid, username, true).toBundle() } }
            2 -> { followingFragmentProvider.get().apply { arguments = FollowingFragmentArgs(caid, username, true).toBundle() } }

            else -> throw IllegalStateException("Unknown exception")
        }

    override fun getCount() = NUM_DETAILS_TABS

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> resources.getString(R.string.stage_broadcast_about_tab)
            1 -> resources.getString(R.string.stage_broadcast_followers_tab)
            else -> resources.getString(R.string.stage_broadcast_following_tab)
        }
    }
}
