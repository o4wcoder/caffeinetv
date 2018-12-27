package tv.caffeine.app.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import tv.caffeine.app.util.DispatchConfig
import kotlin.coroutines.CoroutineContext

open class CaffeineViewModel(val dispatchConfig: DispatchConfig) : ViewModel(), CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + dispatchConfig.main

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }
}
