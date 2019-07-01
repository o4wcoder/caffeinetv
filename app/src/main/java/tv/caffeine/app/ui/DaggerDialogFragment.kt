package tv.caffeine.app.ui

import android.content.Context
import androidx.fragment.app.DialogFragment
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

open class DaggerDialogFragment : DialogFragment(), HasAndroidInjector {

    @Inject lateinit var childFragmentInjector: DispatchingAndroidInjector<Any>
    override fun androidInjector(): AndroidInjector<Any> = childFragmentInjector

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }
}
