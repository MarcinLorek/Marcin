package pl.mlorek.gpwshort.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import pl.mlorek.gpwshort.data.local.dao.HolderPositionDao
import pl.mlorek.gpwshort.data.local.dao.IssuerDao
import pl.mlorek.gpwshort.data.local.dao.PositionSnapshotDao
import pl.mlorek.gpwshort.data.local.dao.SyncMetaDao
import pl.mlorek.gpwshort.data.local.dao.WatchlistDao
import pl.mlorek.gpwshort.data.local.entity.HolderPositionEntity
import pl.mlorek.gpwshort.data.local.entity.IssuerEntity
import pl.mlorek.gpwshort.data.local.entity.PositionSnapshotEntity
import pl.mlorek.gpwshort.data.local.entity.SyncMetaEntity
import pl.mlorek.gpwshort.data.local.entity.WatchlistEntity

@Database(
    entities = [
        IssuerEntity::class,
        PositionSnapshotEntity::class,
        HolderPositionEntity::class,
        WatchlistEntity::class,
        SyncMetaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun issuerDao(): IssuerDao
    abstract fun positionSnapshotDao(): PositionSnapshotDao
    abstract fun holderPositionDao(): HolderPositionDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun syncMetaDao(): SyncMetaDao

    companion object {
        const val DATABASE_NAME = "gpw_short.db"
    }
}
