package tv.caffeine.app.ui

import tv.caffeine.app.di.ViewModelFactory
import javax.inject.Inject

open class CaffeineDialogFragment : DaggerDialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory
}
