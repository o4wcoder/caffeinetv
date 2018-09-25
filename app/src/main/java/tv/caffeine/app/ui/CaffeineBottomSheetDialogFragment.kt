package tv.caffeine.app.ui

import androidx.lifecycle.ViewModelProviders
import tv.caffeine.app.di.ViewModelFactory
import javax.inject.Inject

open class CaffeineBottomSheetDialogFragment : DaggerBottomSheetDialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    protected val viewModelProvider by lazy { ViewModelProviders.of(this, viewModelFactory) }
}