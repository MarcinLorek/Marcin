package pl.mlorek.gpwshort.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Sumaryczna pozycja krótka netto emitenta w konkretnym dniu publikacji KNF.
 * Jeden wiersz per (issuer, dzień publikacji) – zapisywany przy każdym imporcie,
 * co pozwala budować historię nawet jeśli KNF udostępnia wyłącznie stan bieżący.
 */
@Entity(
    tableName = "position_snapshot",
    foreignKeys = [
        ForeignKey(
            entity = IssuerEntity::class,
            parentColumns = ["id"],
            childColumns = ["issuer_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["issuer_id", "publication_epoch_day"], unique = true),
        Index(value = ["issuer_id"]),
    ],
)
data class PositionSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "issuer_id")
    val issuerId: Long,
    @ColumnInfo(name = "publication_epoch_day")
    val publicationEpochDay: Long,
    @ColumnInfo(name = "total_short_percent")
    val totalShortPercent: Double,
    @ColumnInfo(name = "holder_count")
    val holderCount: Int,
    @ColumnInfo(name = "fetched_at_epoch_millis")
    val fetchedAtEpochMillis: Long,
)
