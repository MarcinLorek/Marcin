package pl.mlorek.gpwshort.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Pojedynczy wiersz (id = 0) przechowujący metadane ostatniej udanej synchronizacji. */
@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "last_success_epoch_millis")
    val lastSuccessEpochMillis: Long,
    @ColumnInfo(name = "last_publication_epoch_day")
    val lastPublicationEpochDay: Long,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
