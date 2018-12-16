package tv.caffeine.app

import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.iid.FirebaseInstanceId
import timber.log.Timber
import tv.caffeine.app.auth.LandingFragment
import tv.caffeine.app.databinding.ActivityMainBinding
import tv.caffeine.app.settings.SettingsFragment
import tv.caffeine.app.util.*

private val destinationsWithCustomToolbar = arrayOf(R.id.lobbyFragment, R.id.landingFragment, R.id.stageFragment, R.id.needsUpdateFragment)

class MainActivity : AppCompatActivity() {

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
    }

    override fun onSupportNavigateUp() = navController.navigateUp()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        supportFragmentManager.fragments
                .flatMap { it.childFragmentManager.fragments }
                .find { it is LandingFragment || it is SettingsFragment }
                ?.let {  fragment ->
                    fragment.onActivityResult(requestCode, resultCode, data)
                }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (navController.currentDestination?.id == R.id.stageFragment) {
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
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private fun openNoNetworkFragment() = runOnUiThread { navController.navigateToNoNetwork() }

    private fun closeNoNetworkFragment() = runOnUiThread { navController.closeNoNetwork() }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onUnavailable() {
            openNoNetworkFragment()
        }

        override fun onAvailable(network: Network?) {
            if (isNetworkAvailable()) {
                closeNoNetworkFragment()
            }
        }

        override fun onLost(network: Network?) {
            if (!isNetworkAvailable()) {
                openNoNetworkFragment()
            }
        }
    }
}
