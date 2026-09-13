package pl.mlorek.gpwshort.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Znacząca (≥0,5%) pozycja krótka pojedynczego podmiotu na danego emitenta w danym dniu.
 * Unikalność (issuer, podmiot, data, wartość) implementowana jako unique index +
 * OnConflictStrategy.IGNORE przy imporcie – patrz KnfRemoteDataSource / repozytorium.
 */
@Entity(
    tableName = "holder_position",
    foreignKeys = [
        ForeignKey(
            entity = IssuerEntity::class,
            parentColumns = ["id"],
            childColumns = ["issuer_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["issuer_id", "holder_name", "position_epoch_day", "percent"], unique = true),
        Index(value = ["issuer_id"]),
    ],
)
data class HolderPositionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "issuer_id")
    val issuerId: Long,
    @ColumnInfo(name = "holder_name")
    val holderName: String,
    val percent: Double,
    @ColumnInfo(name = "position_epoch_day")
    val positionEpochDay: Long,
)
