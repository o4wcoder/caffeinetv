package tv.caffeine.app.ui

import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import dagger.android.support.DaggerFragment
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import timber.log.Timber
import tv.caffeine.app.api.isTokenExpirationError
import tv.caffeine.app.api.isVersionCheckError
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.di.ViewModelFactory
import tv.caffeine.app.util.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

open class CaffeineFragment : DaggerFragment(), CoroutineScope {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var dispatchConfig: DispatchConfig

    protected val viewModelProvider by lazy { ViewModelProviders.of(this, viewModelFactory) }

    protected lateinit var job: Job
    private val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        Timber.e(throwable, "Coroutine throwable")
    }

    override val coroutineContext: CoroutineContext
        get() = dispatchConfig.main + job + exceptionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    inline fun <T> handle(result: CaffeineResult<T>, crossinline block: (value: T) -> Unit) {
        when (result) {
            is CaffeineResult.Success -> block(result.value)
            is CaffeineResult.Error -> handleError(result)
            is CaffeineResult.Failure -> handleFailure(result)
        }
    }

    fun <T> handleError(result: CaffeineResult.Error<T>) {
        when {
            result.error.isTokenExpirationError() -> findNavController().navigateToLanding()
            result.error.isVersionCheckError() -> findNavController().navigateToNeedsUpdate()
            else -> Timber.e("Error ${result.error}")
        }
    }

    fun <T> handleFailure(result: CaffeineResult.Failure<T>) {
        Timber.e(result.throwable)
        if (context?.isNetworkAvailable() == false) {
            findNavController().navigateToNoNetwork()
        }
    }
}
