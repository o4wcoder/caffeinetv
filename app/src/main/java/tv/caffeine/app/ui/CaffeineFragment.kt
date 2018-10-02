package tv.caffeine.app.ui

import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import dagger.android.support.DaggerFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import tv.caffeine.app.di.ViewModelFactory
import javax.inject.Inject

open class CaffeineFragment : DaggerFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    protected val viewModelProvider by lazy { ViewModelProviders.of(this, viewModelFactory) }

    protected lateinit var job: Job
    protected val fragmentScope get() = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}