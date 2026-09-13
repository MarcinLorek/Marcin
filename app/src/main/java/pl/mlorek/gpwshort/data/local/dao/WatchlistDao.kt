package pl.mlorek.gpwshort.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pl.mlorek.gpwshort.data.local.entity.WatchlistEntity

@Dao
interface WatchlistDao {

    @Query("SELECT * FROM watchlist")
    fun observeAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE issuer_id = :issuerId LIMIT 1")
    suspend fun find(issuerId: Long): WatchlistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE issuer_id = :issuerId")
    suspend fun delete(issuerId: Long)

    @Query("UPDATE watchlist SET last_notified_snapshot_id = :snapshotId WHERE issuer_id = :issuerId")
    suspend fun markNotified(issuerId: Long, snapshotId: Long)
}
