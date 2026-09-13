package pl.mlorek.gpwshort.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import pl.mlorek.gpwshort.data.local.entity.IssuerEntity

@Dao
interface IssuerDao {

    @Query("SELECT * FROM issuer ORDER BY name ASC")
    fun observeAll(): Flow<List<IssuerEntity>>

    @Query("SELECT * FROM issuer WHERE id = :id")
    fun observeById(id: Long): Flow<IssuerEntity?>

    @Query("SELECT * FROM issuer WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): IssuerEntity?

    @Insert
    suspend fun insert(issuer: IssuerEntity): Long

    @Update
    suspend fun update(issuer: IssuerEntity)

    @Query(
        "UPDATE issuer SET is_closed = 0 WHERE id IN (:ids) AND is_closed = 1",
    )
    suspend fun reopen(ids: List<Long>)

    @Query(
        "UPDATE issuer SET is_closed = 1 WHERE id NOT IN (:openIssuerIds) AND is_closed = 0",
    )
    suspend fun markMissingAsClosed(openIssuerIds: List<Long>)

    @Query("SELECT id FROM issuer")
    suspend fun allIds(): List<Long>
}
