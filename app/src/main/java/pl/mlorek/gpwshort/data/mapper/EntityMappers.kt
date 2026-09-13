package pl.mlorek.gpwshort.data.mapper

import pl.mlorek.gpwshort.data.local.entity.HolderPositionEntity
import pl.mlorek.gpwshort.data.local.entity.IssuerEntity
import pl.mlorek.gpwshort.data.local.entity.PositionSnapshotEntity
import pl.mlorek.gpwshort.domain.model.HolderPosition
import pl.mlorek.gpwshort.domain.model.Issuer
import pl.mlorek.gpwshort.domain.model.IssuerSummary
import pl.mlorek.gpwshort.domain.model.PositionHistoryPoint
import java.time.LocalDate

/**
 * Czyste funkcje mapujące encje Room -> modele domenowe. Wydzielone z repozytorium,
 * żeby dało się je testować bez uruchamiania prawdziwej bazy Room (patrz testy w
 * app/src/test/.../data/repository).
 */

fun epochDayToLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

fun IssuerEntity.toDomain(): Issuer = Issuer(id = id, name = name, isin = isin)

fun buildSummaries(
    issuers: List<IssuerEntity>,
    snapshots: List<PositionSnapshotEntity>,
): Map<Long, IssuerSummary> {
    val snapshotsByIssuer = snapshots.groupBy { it.issuerId }
    val result = mutableMapOf<Long, IssuerSummary>()
    for (issuer in issuers) {
        val issuerSnapshots = snapshotsByIssuer[issuer.id]
            ?.sortedByDescending { it.publicationEpochDay }
            ?: continue
        val latest = issuerSnapshots.firstOrNull() ?: continue
        val previous = issuerSnapshots.getOrNull(1)
        result[issuer.id] = IssuerSummary(
            issuer = issuer.toDomain(),
            totalShortPercent = latest.totalShortPercent,
            holderCount = latest.holderCount,
            positionDate = epochDayToLocalDate(latest.publicationEpochDay),
            previousTotalShortPercent = previous?.totalShortPercent,
            isClosed = issuer.isClosed,
        )
    }
    return result
}

fun buildHistory(snapshots: List<PositionSnapshotEntity>): List<PositionHistoryPoint> {
    val sorted = snapshots.sortedBy { it.publicationEpochDay }
    return sorted.mapIndexed { index, snapshot ->
        val previous = sorted.getOrNull(index - 1)
        PositionHistoryPoint(
            date = epochDayToLocalDate(snapshot.publicationEpochDay),
            totalShortPercent = snapshot.totalShortPercent,
            holderCount = snapshot.holderCount,
            changeVsPrevious = previous?.let { snapshot.totalShortPercent - it.totalShortPercent },
        )
    }
}

/** Dla każdego podmiotu bierze jego najnowszy zgłoszony wpis, z deltą do poprzedniego. */
fun buildLatestHolderPositions(holders: List<HolderPositionEntity>): List<HolderPosition> {
    return holders
        .groupBy { it.holderName }
        .map { (name, entries) ->
            val sorted = entries.sortedByDescending { it.positionEpochDay }
            val latest = sorted.first()
            val previous = sorted.getOrNull(1)
            HolderPosition(
                holderName = name,
                percent = latest.percent,
                positionDate = epochDayToLocalDate(latest.positionEpochDay),
                previousPercent = previous?.percent,
            )
        }
        .sortedByDescending { it.percent }
}
