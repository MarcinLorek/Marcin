package pl.mlorek.gpwshort.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "watchlist",
    foreignKeys = [
        ForeignKey(
            entity = IssuerEntity::class,
            parentColumns = ["id"],
            childColumns = ["issuer_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class WatchlistEntity(
    @PrimaryKey
    @ColumnInfo(name = "issuer_id")
    val issuerId: Long,
    @ColumnInfo(name = "threshold_pp")
    val thresholdPercentagePoints: Double,
    @ColumnInfo(name = "added_at_epoch_millis")
    val addedAtEpochMillis: Long,
    @ColumnInfo(name = "last_notified_snapshot_id")
    val lastNotifiedSnapshotId: Long? = null,
)
