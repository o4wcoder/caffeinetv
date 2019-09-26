package tv.caffeine.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.iid.FirebaseInstanceId
import com.squareup.seismic.ShakeDetector
import dagger.android.support.DaggerAppCompatActivity
import timber.log.Timber
import tv.caffeine.app.analytics.Analytics
import tv.caffeine.app.analytics.AnalyticsEvent
import tv.caffeine.app.analytics.NotificationEvent
import tv.caffeine.app.analytics.Profiling
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.databinding.ActivityMainBinding
import tv.caffeine.app.di.ViewModelFactory
import tv.caffeine.app.ext.expand
import tv.caffeine.app.feature.DevOptionsDialog
import tv.caffeine.app.feature.Feature
import tv.caffeine.app.feature.FeatureConfig
import tv.caffeine.app.profile.MyProfileViewModel
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.settings.SecureSettingsStorage
import tv.caffeine.app.ui.BottomNavigationAvatar
import tv.caffeine.app.util.closeNoNetwork
import tv.caffeine.app.util.dismissKeyboard
import tv.caffeine.app.util.isNetworkAvailable
import tv.caffeine.app.util.navigateToNoNetwork
import tv.caffeine.app.util.notificationId
import tv.caffeine.app.util.notificationTag
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.util.safeUnregisterNetworkCallback
import tv.caffeine.app.util.setDarkMode
import tv.caffeine.app.util.setImmersiveMode
import tv.caffeine.app.util.setImmersiveSticky
import tv.caffeine.app.util.setNavigationBarDarkMode
import tv.caffeine.app.util.unsetImmersiveSticky
import javax.inject.Inject

private val destinationsWithCustomToolbar = arrayOf(
    R.id.lobbySwipeFragment,
    R.id.landingFragment,
    R.id.signInFragment,
    R.id.signUpFragment,
    R.id.forgotFragment,
    R.id.mfaCodeFragment,
    R.id.stagePagerFragment,
    R.id.needsUpdateFragment,
    R.id.friendsWatchingFragment,
    R.id.sendDigitalItemFragment,
    R.id.confirmEmailFragment
)

private val destinationWithReleaseToolbar = arrayOf(
    R.id.lobbySwipeFragment,
    R.id.trendingLobbyFragment,
    R.id.featuredProgramGuideFragment,
    R.id.myProfileFragment
)

private val destinationsWithoutBottomNavBar = arrayOf(
    R.id.landingFragment,
    R.id.signInFragment,
    R.id.signUpFragment,
    R.id.forgotFragment,
    R.id.mfaCodeFragment,
    R.id.stagePagerFragment,
    R.id.needsUpdateFragment,
    R.id.friendsWatchingFragment,
    R.id.sendDigitalItemFragment,
    R.id.confirmEmailFragment
)

private val destinationsAsDialog = arrayOf(
    R.id.reportOrIgnoreDialogFragment,
    R.id.unfollowUserDialogFragment
)

private val destinationsWithDynamicLabel = arrayOf(R.id.lobbyDetailFragment)

private val destinationInPortrait = arrayOf(R.id.landingFragment, R.id.signUpFragment)

private val destinationInImmersiveMode = arrayOf(R.id.stagePagerFragment)

private const val OPEN_NO_NETWORK_FRAGMENT_DELAY_MS = 5000L
@VisibleForTesting const val SCREEN_TITLE = "screenTitle"

class MainActivity : DaggerAppCompatActivity(), ShakeDetector.Listener {

    @Inject lateinit var profiling: Profiling
    @Inject lateinit var analytics: Analytics
    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var firebaseAnalytics: FirebaseAnalytics
    @Inject lateinit var firebaseInstanceId: FirebaseInstanceId
    @Inject lateinit var featureConfig: FeatureConfig
    @Inject lateinit var releaseDesignConfig: ReleaseDesignConfig
    @Inject lateinit var secureSettingsStorage: SecureSettingsStorage

    private lateinit var navController: NavController
    @VisibleForTesting lateinit var binding: ActivityMainBinding

    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val myProfileViewModel: MyProfileViewModel by viewModels { viewModelFactory }
    private lateinit var bottomNavigationAvatar: BottomNavigationAvatar
    private var isDevOptionsOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        navController = findNavController(R.id.activity_main)
        if (releaseDesignConfig.isReleaseDesignActive()) {
            setTheme(R.style.AppTheme_Release)
        }
        setAppBar(binding, navController)
        setBottomNavigation(binding)
        setShakeDetector()

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            updateUiOnDestinationChange(destination.id, binding, arguments)
            firebaseAnalytics.setCurrentScreen(this, destination.label.toString(), null)
        }
        firebaseInstanceId.instanceId.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Timber.d("FCM registration token retrieved")
                task.result?.let { result ->
                    Timber.d("${result.id}, ${result.token}")
                    if (secureSettingsStorage.firebaseToken == null) {
                        secureSettingsStorage.firebaseToken = result.token
                    }
                }
            } else {
                Timber.e("Couldn't get the FCM registration token")
            }
        }
        if (savedInstanceState == null) {
            profiling.initialize()
            createNotificationChannel()
            if (intent.notificationId != null && intent.notificationTag != null) {
                val notificationEvent = NotificationEvent(NotificationEvent.Type.Opened, intent.notificationId, intent.notificationTag)
                analytics.trackEvent(AnalyticsEvent.Notification(tokenStore.caid, notificationEvent))
            }
        }
    }

    private fun setAppBar(binding: ActivityMainBinding, navController: NavController) {
        setSupportActionBar(binding.activityToolbar)
        setupActionBarWithNavController(this, navController)
        binding.releaseAppBar.navController = navController
        if (featureConfig.isFeatureEnabled(Feature.DEV_OPTIONS)) {
            binding.releaseAppBar.setWordmarkLongClickListener { launchDevOptions() }
        }
    }

    private fun setBottomNavigation(binding: ActivityMainBinding) {
        bottomNavigationAvatar = BottomNavigationAvatar(
            applicationContext, binding.bottomNavigation.menu.findItem(R.id.bottom_nav_profile_menu_item))
        val isReleaseDesign = releaseDesignConfig.isReleaseDesignActive()
        if (isReleaseDesign) {
            // The default tint list will apply a solid color to the avatar. Set to null before loading.
            binding.bottomNavigation.itemIconTintList = null
            loadBottomNavigationAvatar()
            binding.bottomNavigation.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)
        }
        // The system navigation bar should be dark so we don't have two white bars
        setNavigationBarDarkMode(isReleaseDesign)
    }

    private fun setShakeDetector() {
        if (featureConfig.isFeatureEnabled(Feature.DEV_OPTIONS)) {
            getSystemService<SensorManager>()?.let { sensorManager ->
                ShakeDetector(this).start(sensorManager)
            }
        }
    }

    @VisibleForTesting
    fun updateUiOnDestinationChange(destinationId: Int, binding: ActivityMainBinding, arguments: Bundle? = null) {
        dismissKeyboard()
        val isReleaseDesign = releaseDesignConfig.isReleaseDesignActive()
        if (destinationId !in destinationsAsDialog) {
            // Keep both bars as they are if we are navigating to a dialog.
            binding.releaseAppBar.isVisible =
                isReleaseDesign && destinationId in destinationWithReleaseToolbar
            binding.classicAppBar.isVisible = destinationId !in destinationsWithCustomToolbar &&
                !binding.releaseAppBar.isVisible
            binding.bottomNavigation.isVisible = isReleaseDesign &&
                destinationId !in destinationsWithoutBottomNavBar
            updateBottomNavigationStatus(binding.bottomNavigation, destinationId)
        }
        if (destinationId in destinationsWithDynamicLabel) {
            arguments?.getString(SCREEN_TITLE)?.let {
                supportActionBar?.title = it
            }
        }

        requestedOrientation = if (destinationId in destinationInPortrait) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        updateImmersiveMode(destinationId)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "general"
            val channelName = getString(R.string.notification_channel_general_name)
            val channelDescription = getString(R.string.notification_channel_general_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }
            getSystemService<NotificationManager>()?.createNotificationChannel(channel)
        }
    }

    override fun onSupportNavigateUp() = navController.navigateUp()

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (navController.currentDestination?.id == R.id.stageFragment) {
            /**
             * Samsung Galaxy S9 has an overlay as a part of the fullscreen NUX (new user expereince) that
             * constantly takes focus from the foreground app. It results in a flickering screen on stage
             * because we rely on Activity.onWindowFocusChanged() to detect the system dialog and keyboard,
             * and adjust the immersive mode. We disable this function when the user first enters the stage
             * and hopefully they finish the Samsung NUX.
             */
            getPreferences(Context.MODE_PRIVATE)?.let {
                val key = getString(R.string.is_first_time_on_stage)
                if (it.getBoolean(key, true)) {
                    return
                }
            }
            if (hasFocus) {
                setImmersiveSticky()
            } else {
                unsetImmersiveSticky()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startConnectivityMonitoring()
    }

    override fun onStop() {
        super.onStop()
        stopConnectivityMonitoring()
    }

    private fun startConnectivityMonitoring() {
        val connectivityManager = getSystemService<ConnectivityManager>() ?: return
        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback)
        if (!isNetworkAvailable()) openNoNetworkFragment()
    }

    private fun stopConnectivityMonitoring() {
        val connectivityManager = getSystemService<ConnectivityManager>() ?: return
        connectivityManager.safeUnregisterNetworkCallback(networkCallback)
    }

    private fun openNoNetworkFragment() = runOnUiThread { navController.navigateToNoNetwork() }

    private fun closeNoNetworkFragment() = runOnUiThread { navController.closeNoNetwork() }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        private val handler = Handler()
        private val runnable = Runnable {
            openNoNetworkFragment()
        }

        override fun onAvailable(network: Network?) {
            if (isNetworkAvailable()) {
                handler.removeCallbacks(runnable)
                closeNoNetworkFragment()
            }
        }

        override fun onLost(network: Network?) {
            if (!isNetworkAvailable()) {
                handler.postDelayed(runnable, OPEN_NO_NETWORK_FRAGMENT_DELAY_MS)
            }
        }
    }

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { menuItem ->
        val destinationId = when (menuItem.itemId) {
            R.id.bottom_nav_star_menu_item -> R.id.lobbySwipeFragment
            R.id.bottom_nav_flame_menu_item -> R.id.trendingLobbyFragment
            R.id.bottom_nav_clock_menu_item -> R.id.featuredProgramGuideFragment
            R.id.bottom_nav_profile_menu_item -> R.id.myProfileFragment
            else -> -1
        }
        if (navController.currentDestination?.id != destinationId) {
            navController.safeNavigate(destinationId)
        }
        true
    }

    @VisibleForTesting fun updateBottomNavigationStatus(bottomNavigationView: BottomNavigationView, destinationId: Int) {
        val menuItemId = when (destinationId) {
            R.id.lobbySwipeFragment -> R.id.bottom_nav_star_menu_item
            R.id.trendingLobbyFragment -> R.id.bottom_nav_flame_menu_item
            R.id.featuredProgramGuideFragment -> R.id.bottom_nav_clock_menu_item
            R.id.myProfileFragment -> R.id.bottom_nav_profile_menu_item
            else -> -1
        }
        if (menuItemId != -1) {
            bottomNavigationView.isSelected = false
            bottomNavigationView.menu.findItem(menuItemId)?.isChecked = true
        }
        bottomNavigationAvatar.updateSelectedState()
    }

    @VisibleForTesting
    fun updateImmersiveMode(destinationId: Int) {

        if (destinationId in destinationInImmersiveMode) {
            setImmersiveMode()
        } else if (destinationId !in destinationsAsDialog) {
            unsetImmersiveMode()
        }
    }

    private fun loadBottomNavigationAvatar() {
        myProfileViewModel.userProfile.observe(this, Observer { userProfile ->
            bottomNavigationAvatar.loadAvatar(userProfile.avatarImageUrl)
        })
    }

    fun reloadMyProfileViewModelOnSign() = myProfileViewModel.load()

    override fun hearShake() {
        if (!isDevOptionsOpen) {
            isDevOptionsOpen = true
            launchDevOptions()
        }
    }

    private fun launchDevOptions() {
        DevOptionsDialog(this).apply {
            expand()
            show()
            setOnDismissListener {
                isDevOptionsOpen = false
            }
        }
    }

    @VisibleForTesting
    fun unsetImmersiveMode() {
        unsetImmersiveSticky()
        setDarkMode(false)
        if (releaseDesignConfig.isReleaseDesignActive()) {
            setNavigationBarDarkMode(true)
        }

        getPreferences(Context.MODE_PRIVATE)?.let {
            val key = getString(R.string.is_first_time_on_stage)
            if (it.getBoolean(key, true)) {
                // This will re-enable the immersive mode function in MainActivity.onWindowFocusChanged().
                it.edit().putBoolean(key, false).apply()
            }
        }
    }
}
