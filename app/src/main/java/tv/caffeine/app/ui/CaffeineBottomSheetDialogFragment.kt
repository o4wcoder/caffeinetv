package tv.caffeine.app.ui

import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import tv.caffeine.app.di.ViewModelFactory
import tv.caffeine.app.util.DispatchConfig
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

open class CaffeineBottomSheetDialogFragment : DaggerBottomSheetDialogFragment(), CoroutineScope {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var dispatchConfig: DispatchConfig

    protected val viewModelProvider by lazy { ViewModelProviders.of(this, viewModelFactory) }

    protected lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = dispatchConfig.main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = SupervisorJob()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
