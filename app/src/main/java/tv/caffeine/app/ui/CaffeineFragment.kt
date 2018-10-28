package tv.caffeine.app.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.android.support.DaggerFragment
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.isTokenExpirationError
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.di.ViewModelFactory
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

open class CaffeineFragment : DaggerFragment(), CoroutineScope {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    protected val viewModelProvider by lazy { ViewModelProviders.of(this, viewModelFactory) }

    protected lateinit var job: Job
    private val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        Timber.e(throwable, "Coroutine exception")
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job + exceptionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    inline fun <T> handle(result: CaffeineResult<T>, view: View, crossinline block: (value: T) -> Unit) {
        when (result) {
            is CaffeineResult.Success -> block(result.value)
            is CaffeineResult.Error -> {
                if (result.error.isTokenExpirationError()) {
                    findNavController().popBackStack()
                    findNavController().navigate(R.id.action_global_landingFragment)
                } else {
                    Snackbar.make(view, "Error ${result.error}", Snackbar.LENGTH_SHORT).show()
                }
            }
            is CaffeineResult.Failure -> {
                val e = result.exception
                Timber.e(e, "Failure in the LobbyFragment")
                Snackbar.make(view, "Failure $e", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

}
