package tv.caffeine.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.iid.FirebaseInstanceId
import dagger.android.support.DaggerAppCompatActivity
import timber.log.Timber
import tv.caffeine.app.analytics.Analytics
import tv.caffeine.app.analytics.AnalyticsEvent
import tv.caffeine.app.analytics.NotificationEvent
import tv.caffeine.app.analytics.Profiling
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.databinding.ActivityMainBinding
import tv.caffeine.app.util.*
import javax.inject.Inject

private val destinationsWithCustomToolbar = arrayOf(R.id.lobbyFragment, R.id.landingFragment, R.id.stageFragment, R.id.needsUpdateFragment)

private const val OPEN_NO_NETWORK_FRAGMENT_DELAY_MS = 5000L

class MainActivity : DaggerAppCompatActivity() {

    @Inject lateinit var profiling: Profiling
    @Inject lateinit var analytics: Analytics
    @Inject lateinit var tokenStore: TokenStore

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        setSupportActionBar(binding.activityToolbar)
        navController = findNavController(R.id.activity_main)
        setupActionBarWithNavController(this, navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            dismissKeyboard()
            binding.activityAppbar.isVisible = destination.id !in destinationsWithCustomToolbar
            FirebaseAnalytics.getInstance(this).setCurrentScreen(this, destination.label.toString(), null)
        }
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Timber.d("FCM registration token retrieved")
                task.result?.let { result ->
                    Timber.d("${result.id}, ${result.token}")
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
}
