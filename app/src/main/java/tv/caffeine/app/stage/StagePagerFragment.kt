package tv.caffeine.app.stage

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.google.firebase.analytics.FirebaseAnalytics
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import org.threeten.bp.Clock
import tv.caffeine.app.R
import tv.caffeine.app.analytics.FirebaseEvent
import tv.caffeine.app.analytics.logEvent
import tv.caffeine.app.analytics.logScreen
import tv.caffeine.app.api.ApiErrorResult
import tv.caffeine.app.api.VersionCheckError
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.databinding.FragmentStagePagerBinding
import tv.caffeine.app.lobby.LobbyViewModel
import tv.caffeine.app.lobby.toDistinctLiveBroadcasters
import tv.caffeine.app.lobby.type.Page
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.session.SessionCheckViewModel
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.ViewPagerLogOnPageChangeListener
import tv.caffeine.app.update.IsVersionSupportedCheckUseCase
import tv.caffeine.app.util.broadcasterUsername
import tv.caffeine.app.util.isNetworkAvailable
import tv.caffeine.app.util.safeUnregisterNetworkCallback
import tv.caffeine.app.webrtc.SurfaceViewRendererTuner
import javax.inject.Inject

private const val BUNDLE_KEY_BROADCASTERS = "broadcasters"

class StagePagerFragment @Inject constructor(
    private val isVersionSupportedCheckUseCase: IsVersionSupportedCheckUseCase,
    private val adapterFactory: StagePagerAdapter.Factory,
    private val followManager: FollowManager,
    private val releaseDesignConfig: ReleaseDesignConfig,
    private val firebaseAnalytics: FirebaseAnalytics
) : CaffeineFragment(R.layout.fragment_stage_pager) {

    @VisibleForTesting var binding: FragmentStagePagerBinding? = null
    private val args by navArgs<StagePagerFragmentArgs>()
    private val sessionCheckViewModel: SessionCheckViewModel by viewModels { viewModelFactory }
    private val lobbyViewModel: LobbyViewModel by viewModels { viewModelFactory }
    @VisibleForTesting var broadcasters = listOf<String>()
    private var stagePagerAdapter: StagePagerAdapter? = null
        set(value) {
            binding?.stageViewPager?.adapter = value
            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseAnalytics.logScreen(this)
        binding = FragmentStagePagerBinding.bind(view)
        val swipeButtonOnClickListener = View.OnClickListener {
            binding?.stageViewPager?.apply {
                if (currentItem + 1 < adapter?.count ?: 0) {
                    setCurrentItem(currentItem + 1, true)
                    firebaseAnalytics.logEvent(FirebaseEvent.StageSwipeButtonClicked)
                }
            }
        }
        savedInstanceState?.getStringArrayList(BUNDLE_KEY_BROADCASTERS)?.let { broadcasters = it }
        if (broadcasters.isEmpty()) {
            setupAdapter(swipeButtonOnClickListener)
        } else {
            // The view pager restores the index.
            stagePagerAdapter = adapterFactory.create(childFragmentManager, broadcasters, swipeButtonOnClickListener)
        }
    }

    override fun onResume() {
        super.onResume()
        context?.getSystemService<ConnectivityManager>()?.registerNetworkCallback(
            NetworkRequest.Builder().build(), networkCallback)
    }

    override fun onPause() {
        context?.getSystemService<ConnectivityManager>()?.safeUnregisterNetworkCallback(networkCallback)
        super.onPause()
    }

    @VisibleForTesting fun setupAdapter(swipeButtonOnClickListener: View.OnClickListener) {
        val currentUsername = followManager.currentUserDetails()?.username
        if (currentUsername != null && currentUsername == args.broadcasterUsername()) {
            // disable swiping between broadcasts if the user lands on their own stage
            setBroadcastersAndAdapter(listOf(currentUsername), swipeButtonOnClickListener)
        } else {
            val allDistinctLiveBroadcasters = args.broadcasters
            if (allDistinctLiveBroadcasters != null) {
                val (configuredBroadcasters, index) = configureBroadcasters(
                    args.broadcasterUsername(), allDistinctLiveBroadcasters.toList()
                )
                setBroadcastersAndAdapter(configuredBroadcasters, swipeButtonOnClickListener, index)
                binding?.stageViewPager?.addOnPageChangeListener(ViewPagerLogOnPageChangeListener(index, firebaseAnalytics))
            } else {
                loadAndSetBroadcastersAndAdapter(swipeButtonOnClickListener)
                binding?.stageViewPager?.addOnPageChangeListener(ViewPagerLogOnPageChangeListener(0, firebaseAnalytics))
            }
        }
    }

    private fun loadAndSetBroadcastersAndAdapter(swipeButtonOnClickListener: View.OnClickListener) {
        if (releaseDesignConfig.isReleaseDesignActive()) {
            lobbyViewModel.refreshV5(page = Page.HOME) {}
            lobbyViewModel.lobbyV5.observe(this, Observer {
                handle(it) { lobby ->
                    val (configuredBroadcasters, index) = configureBroadcasters(
                        args.broadcasterUsername(), lobby.pagePayload.toDistinctLiveBroadcasters()
                    )
                    setBroadcastersAndAdapter(configuredBroadcasters, swipeButtonOnClickListener, index)
                }
            })
        } else {
            lobbyViewModel.refresh()
            lobbyViewModel.lobby.observe(this, Observer {
                handle(it) { lobby ->
                    val (configuredBroadcasters, index) = configureBroadcasters(
                        args.broadcasterUsername(), lobby.getAllBroadcasters()
                    )
                    setBroadcastersAndAdapter(configuredBroadcasters, swipeButtonOnClickListener, index)
                }
            })
        }
    }

    private fun setBroadcastersAndAdapter(
        broadcasters: List<String>,
        swipeButtonOnClickListener: View.OnClickListener,
        index: Int = 0
    ) {
        this.broadcasters = broadcasters
        stagePagerAdapter = adapterFactory.create(childFragmentManager, broadcasters, swipeButtonOnClickListener)
        binding?.stageViewPager?.currentItem = index
    }

    /**
     * Configure the broadcasters given the initial broadcaster and the lobby broadcasters.
     *
     * @return the configured broadcasters list and the index in a pair.
     */
    @VisibleForTesting fun configureBroadcasters(initialBroadcaster: String, lobbyBroadcasters: List<String>): Pair<List<String>, Int> {
        val initialBroadcasterIndex = lobbyBroadcasters.indexOf(initialBroadcaster)
        val broadcasters = if (initialBroadcasterIndex == -1) {
            listOf(initialBroadcaster).plus(lobbyBroadcasters)
        } else {
            lobbyBroadcasters
        }
        val index = if (initialBroadcasterIndex == -1) 0 else initialBroadcasterIndex
        return Pair(broadcasters, index)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(BUNDLE_KEY_BROADCASTERS, ArrayList(broadcasters))
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
    @Assisted fragmentManager: FragmentManager,
    @Assisted private val broadcasters: List<String>,
    @Assisted private val swipeButtonOnClickListener: View.OnClickListener,
    private val factory: NewReyesController.Factory,
    private val surfaceViewRendererTuner: SurfaceViewRendererTuner,
    private val followManager: FollowManager,
    private val picasso: Picasso,
    private val clock: Clock,
    private val releaseDesignConfig: ReleaseDesignConfig
) : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    @AssistedInject.Factory
    interface Factory {
        fun create(
            fragmentManager: FragmentManager,
            broadcasters: List<String>,
            swipeButtonOnClickListener: View.OnClickListener
        ): StagePagerAdapter
    }

    var currentStage: StageFragment? = null

    override fun getItem(position: Int): Fragment {
        val stageFragment = StageFragment(
            factory, surfaceViewRendererTuner, followManager, picasso, clock, releaseDesignConfig)
        val canSwipe = count > 1 && position < count - 1
        stageFragment.arguments = StageFragmentArgs(broadcasters[position], canSwipe).toBundle()
        stageFragment.swipeButtonOnClickListener = swipeButtonOnClickListener
        currentStage = stageFragment
        return stageFragment
    }

    override fun getCount() = broadcasters.size
}
