package pl.mlorek.gpwshort.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pl.mlorek.gpwshort.data.local.entity.HolderPositionEntity

@Dao
interface HolderPositionDao {

    /** IGNORE na unikalnym (issuer, podmiot, data, %) realizuje wymaganą deduplikację importu. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(holders: List<HolderPositionEntity>): List<Long>

    @Query("SELECT * FROM holder_position WHERE issuer_id = :issuerId ORDER BY position_epoch_day DESC")
    fun observeForIssuer(issuerId: Long): Flow<List<HolderPositionEntity>>
}
