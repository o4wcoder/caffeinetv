package tv.caffeine.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import kotlinx.android.synthetic.main.activity_main.*

private val destinationsWithCustomToolbar = arrayOf(R.id.lobbyFragment, R.id.landingFragment, R.id.stageFragment)

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(activity_toolbar)
        val navController = findNavController(R.id.activity_main)
        setupActionBarWithNavController(this, navController)
        navController.addOnNavigatedListener { _, destination ->
            activity_appbar.isVisible = destination.id !in destinationsWithCustomToolbar
        }
    }

    override fun onSupportNavigateUp() = findNavController(R.id.activity_main).navigateUp()
}
