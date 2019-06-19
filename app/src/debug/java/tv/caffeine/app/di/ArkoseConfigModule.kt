package tv.caffeine.app.di

import dagger.Module
import dagger.Provides
import javax.inject.Named

const val ARKOSE_PUBLIC_KEY = "ARKOSE_PUBLIC_KEY"

@Module
class ArkoseConfigModule {

    @Provides
    @Named(ARKOSE_PUBLIC_KEY)
    fun providesArkosePublicKey() = "FFC14D1E-BDC8-3904-1E09-97E61F96C867"
}