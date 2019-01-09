package tv.caffeine.app.di

import com.squareup.inject.assisted.dagger2.AssistedModule
import dagger.Module

@AssistedModule
@Module(includes = [AssistedInject_CaffeineAssistedModule::class])
interface CaffeineAssistedModule
