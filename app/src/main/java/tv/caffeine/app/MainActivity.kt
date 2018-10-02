package tv.caffeine.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
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
        }
    }

    override fun onSupportNavigateUp() = findNavController(R.id.activity_main).navigateUp()
}
