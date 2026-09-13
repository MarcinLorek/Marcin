package pl.mlorek.gpwshort.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pl.mlorek.gpwshort.data.local.entity.SyncMetaEntity

@Dao
interface SyncMetaDao {

    @Query("SELECT * FROM sync_meta WHERE id = ${SyncMetaEntity.SINGLETON_ID}")
    suspend fun get(): SyncMetaEntity?

    @Query("SELECT * FROM sync_meta WHERE id = ${SyncMetaEntity.SINGLETON_ID}")
    fun observe(): Flow<SyncMetaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: SyncMetaEntity)
}
