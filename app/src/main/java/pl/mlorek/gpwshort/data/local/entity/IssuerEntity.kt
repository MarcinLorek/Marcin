package pl.mlorek.gpwshort.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "issuer",
    indices = [Index(value = ["name"], unique = true)],
)
data class IssuerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isin: String?,
    @ColumnInfo(name = "is_closed")
    val isClosed: Boolean = false,
    /** Ostatnia data publikacji KNF, w której emitent miał otwartą pozycję (epoch day). */
    @ColumnInfo(name = "last_open_epoch_day")
    val lastOpenEpochDay: Long,
)
