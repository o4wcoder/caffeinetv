package tv.caffeine.app.stage

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.view.View
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import org.threeten.bp.Clock
import org.webrtc.EglBase
import org.webrtc.MediaCodecVideoDecoder
import timber.log.Timber
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.VersionCheckError
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.databinding.FragmentStagePagerBinding
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.session.SessionCheckViewModel
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.update.IsVersionSupportedCheckUseCase
import tv.caffeine.app.util.isNetworkAvailable
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.util.safeUnregisterNetworkCallback
import tv.caffeine.app.util.setDarkMode
import tv.caffeine.app.util.setImmersiveSticky
import tv.caffeine.app.util.unsetImmersiveSticky
import javax.inject.Inject

class StagePagerFragment @Inject constructor(
        private val isVersionSupportedCheckUseCase: IsVersionSupportedCheckUseCase,
        private val adapterFactory: StagePagerAdapter.Factory
): CaffeineFragment(R.layout.fragment_stage_pager) {

    private lateinit var binding: FragmentStagePagerBinding
    private val args by navArgs<StagePagerFragmentArgs>()
    private val sessionCheckViewModel: SessionCheckViewModel by viewModels { viewModelFactory }
    private var stagePagerAdapter: StagePagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        if (!MediaCodecVideoDecoder.isH264HwSupported()) {
            Timber.e(Exception("Failed to decode H264"))
            findNavController().safeNavigate(MainNavDirections.actionGlobalHardwareNotSupportedFragment())
            return
        }
        context?.getSystemService<ConnectivityManager>()?.registerNetworkCallback(
                NetworkRequest.Builder().build(), networkCallback)
        sessionCheckViewModel.sessionCheck.observe(this, Observer { result ->
            handle(result) {}
        })
        launch {
            val isVersionSupported = isVersionSupportedCheckUseCase()
            if (isVersionSupported is CaffeineEmptyResult.Error) {
                handleError(CaffeineResult.Error<ApiErrorResult>(VersionCheckError()))
            }
        }
    }

    override fun onDestroy() {
        context?.getSystemService<ConnectivityManager>()?.safeUnregisterNetworkCallback(networkCallback)
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.apply {
            setDarkMode(true)
            setImmersiveSticky()
        }
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentStagePagerBinding.bind(view)
        stagePagerAdapter = adapterFactory.create(this, args.broadcastLink)
        binding.stageViewPager.adapter = stagePagerAdapter
    }

    override fun onDestroyView() {
        activity?.apply {
            unsetImmersiveSticky()
            setDarkMode(false)

            getPreferences(Context.MODE_PRIVATE)?.let {
                val key = getString(R.string.is_first_time_on_stage)
                if (it.getBoolean(key, true)) {
                    // This will re-enable the immersive mode function in MainActivity.onWindowFocusChanged().
                    it.edit().putBoolean(key, false).apply()
                }
            }
        }
        binding.stageViewPager.adapter = null
        stagePagerAdapter = null
        super.onDestroyView()
    }

    private fun connectStage() {
        stagePagerAdapter?.currentStage?.connectStage()
    }

    private fun disconnectStage() {
        stagePagerAdapter?.currentStage?.disconnectStage()
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        private var wasNetworkLost = false

        override fun onAvailable(network: Network?) {
            super.onAvailable(network)
            if (wasNetworkLost) {
                wasNetworkLost = false
                launch {
                    connectStage()
                }
            }
        }

        /**
         * 1. AP on, wifi off -> stage -> wifi on -> AP off -> isNetworkAvailable = true -> connectStage()
         * 2. AP on, wifi on -> stage -> wifi off -> isNetworkAvailable = false -> onAvailable() -> connectStage()
         * 3. Wifi on, AP on/off -> stage -> AP off/on -> no callbacks
         *
         * There is a potential Android bug in scenario #1 after the "wifi on" step.
         * The data is still being funneled through AP, but Android thinks wifi is the active network.
         * When we turn off AP, we need to disconnect the stage on AP and re-connect it on wifi.
         */
        override fun onLost(network: Network?) {
            super.onLost(network)
            wasNetworkLost = true
            disconnectStage()
            if (context?.isNetworkAvailable() == true) {
                launch {
                    connectStage()
                }
            }
        }
    }

}

class StagePagerAdapter @AssistedInject constructor(
        @Assisted fragment: Fragment,
        @Assisted private val broadcasterName: String,
        private val factory: NewReyesController.Factory,
        private val eglBase: EglBase,
        private val followManager: FollowManager,
        private val chatMessageAdapter: ChatMessageAdapter,
        private val friendsWatchingAdapter: FriendsWatchingAdapter,
        private val picasso: Picasso,
        private val clock: Clock
) : FragmentStateAdapter(fragment) {

    @AssistedInject.Factory
    interface Factory {
        fun create(fragment: Fragment, broadcasterName: String): StagePagerAdapter
    }

    var currentStage: StageFragment? = null

    override fun getItem(position: Int): Fragment {
        val stageFragment = StageFragment(
                factory, eglBase, followManager, chatMessageAdapter, friendsWatchingAdapter, picasso, clock)
        stageFragment.arguments = bundleOf("broadcastLink" to broadcasterName)
        currentStage = stageFragment
        return stageFragment
    }

    override fun getItemCount(): Int = 1

}
