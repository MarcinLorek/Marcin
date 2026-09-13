package pl.mlorek.gpwshort.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pl.mlorek.gpwshort.data.local.entity.PositionSnapshotEntity

@Dao
interface PositionSnapshotDao {

    /** Nadpisuje wiersz o tym samym (issuer_id, publication_epoch_day) – patrz unique index. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: PositionSnapshotEntity): Long

    @Query("SELECT * FROM position_snapshot ORDER BY issuer_id ASC, publication_epoch_day DESC")
    fun observeAllDescending(): Flow<List<PositionSnapshotEntity>>

    @Query("SELECT * FROM position_snapshot WHERE issuer_id = :issuerId ORDER BY publication_epoch_day ASC")
    fun observeForIssuer(issuerId: Long): Flow<List<PositionSnapshotEntity>>

    @Query("SELECT MAX(publication_epoch_day) FROM position_snapshot")
    suspend fun latestPublicationEpochDay(): Long?

    @Query("SELECT DISTINCT issuer_id FROM position_snapshot WHERE publication_epoch_day = :epochDay")
    suspend fun issuerIdsForPublicationDay(epochDay: Long): List<Long>

    @Query(
        "SELECT * FROM position_snapshot WHERE issuer_id = :issuerId AND publication_epoch_day = :epochDay LIMIT 1",
    )
    suspend fun findExact(issuerId: Long, epochDay: Long): PositionSnapshotEntity?

    @Query(
        "SELECT * FROM position_snapshot WHERE issuer_id = :issuerId ORDER BY publication_epoch_day DESC LIMIT 1",
    )
    suspend fun latestForIssuer(issuerId: Long): PositionSnapshotEntity?
}
