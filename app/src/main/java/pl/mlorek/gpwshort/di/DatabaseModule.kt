package pl.mlorek.gpwshort.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pl.mlorek.gpwshort.data.local.AppDatabase
import pl.mlorek.gpwshort.data.local.dao.HolderPositionDao
import pl.mlorek.gpwshort.data.local.dao.IssuerDao
import pl.mlorek.gpwshort.data.local.dao.PositionSnapshotDao
import pl.mlorek.gpwshort.data.local.dao.SyncMetaDao
import pl.mlorek.gpwshort.data.local.dao.WatchlistDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME).build()

    @Provides
    fun provideIssuerDao(db: AppDatabase): IssuerDao = db.issuerDao()

    @Provides
    fun providePositionSnapshotDao(db: AppDatabase): PositionSnapshotDao = db.positionSnapshotDao()

    @Provides
    fun provideHolderPositionDao(db: AppDatabase): HolderPositionDao = db.holderPositionDao()

    @Provides
    fun provideWatchlistDao(db: AppDatabase): WatchlistDao = db.watchlistDao()

    @Provides
    fun provideSyncMetaDao(db: AppDatabase): SyncMetaDao = db.syncMetaDao()
}
