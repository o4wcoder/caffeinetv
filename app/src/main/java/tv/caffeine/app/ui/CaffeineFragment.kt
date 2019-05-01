package tv.caffeine.app.ui

import android.content.Context
import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import tv.caffeine.app.api.isTokenExpirationError
import tv.caffeine.app.api.isVersionCheckError
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.di.ViewModelFactory
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.util.isNetworkAvailable
import tv.caffeine.app.util.navigateToLanding
import tv.caffeine.app.util.navigateToNeedsUpdate
import tv.caffeine.app.util.navigateToNoNetwork
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

abstract class CaffeineFragment(@LayoutRes contentLayoutId: Int) : Fragment(contentLayoutId),
        HasSupportFragmentInjector, CoroutineScope {

    @Inject lateinit var childFragmentInjector: DispatchingAndroidInjector<Fragment>

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = childFragmentInjector

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var dispatchConfig: DispatchConfig

    protected lateinit var job: Job
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Coroutine throwable")
    }

    override val coroutineContext: CoroutineContext
        get() = dispatchConfig.main + job + exceptionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = SupervisorJob()
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
