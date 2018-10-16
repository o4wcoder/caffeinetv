package tv.caffeine.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import com.google.firebase.analytics.FirebaseAnalytics
import tv.caffeine.app.auth.LandingFragment
import tv.caffeine.app.databinding.ActivityMainBinding

private val destinationsWithCustomToolbar = arrayOf(R.id.lobbyFragment, R.id.landingFragment, R.id.stageFragment)

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        setSupportActionBar(binding.activityToolbar)
        val navController = findNavController(R.id.activity_main)
        setupActionBarWithNavController(this, navController)
        navController.addOnNavigatedListener { _, destination ->
            binding.activityAppbar.isVisible = destination.id !in destinationsWithCustomToolbar
            FirebaseAnalytics.getInstance(this).setCurrentScreen(this, destination.label.toString(), null)
        }
    }

    override fun onSupportNavigateUp() = findNavController(R.id.activity_main).navigateUp()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        supportFragmentManager.fragments
                .flatMap { it.childFragmentManager.fragments }
                .find { it is LandingFragment }
                ?.let {  landingFragment ->
                    landingFragment.onActivityResult(requestCode, resultCode, data)
                }
    }
}
