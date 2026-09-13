package pl.mlorek.gpwshort.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.mlorek.gpwshort.data.repository.ShortPositionsRepositoryImpl
import pl.mlorek.gpwshort.domain.repository.ShortPositionsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindShortPositionsRepository(
        impl: ShortPositionsRepositoryImpl,
    ): ShortPositionsRepository
}
