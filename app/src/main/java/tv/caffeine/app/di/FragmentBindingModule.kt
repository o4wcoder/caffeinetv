package tv.caffeine.app.di

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import tv.caffeine.app.notifications.NotificationsFragment

@Module
abstract class FragmentBindingModule {

    @Binds
    @IntoMap
    @FragmentKey(NotificationsFragment::class)
    abstract fun bindNotificationsFragment(fragment: NotificationsFragment): Fragment

    @Binds
    abstract fun bindFragmentFactory(factory: InjectingFragmentFactory): FragmentFactory
}

