package tv.caffeine.app.ui

import android.app.Dialog
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import tv.caffeine.app.di.ViewModelFactory
import tv.caffeine.app.util.DispatchConfig
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import tv.caffeine.app.ext.expand

open class CaffeineBottomSheetDialogFragment : DaggerBottomSheetDialogFragment(), CoroutineScope {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var dispatchConfig: DispatchConfig

    protected lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = dispatchConfig.main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = SupervisorJob()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.expand()
        return dialog
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
