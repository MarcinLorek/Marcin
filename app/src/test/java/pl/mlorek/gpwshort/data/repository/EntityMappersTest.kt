package pl.mlorek.gpwshort.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.mlorek.gpwshort.data.local.entity.HolderPositionEntity
import pl.mlorek.gpwshort.data.local.entity.IssuerEntity
import pl.mlorek.gpwshort.data.local.entity.PositionSnapshotEntity
import pl.mlorek.gpwshort.data.mapper.buildHistory
import pl.mlorek.gpwshort.data.mapper.buildLatestHolderPositions
import pl.mlorek.gpwshort.data.mapper.buildSummaries
import java.time.LocalDate

class EntityMappersTest {

    @Test
    fun `buildSummaries computes change vs previous snapshot and excludes issuers without snapshots`() {
        val issuers = listOf(
            IssuerEntity(id = 1, name = "Spółka A", isin = null, lastOpenEpochDay = day(3)),
            IssuerEntity(id = 2, name = "Spółka bez danych", isin = null, lastOpenEpochDay = day(3)),
        )
        val snapshots = listOf(
            PositionSnapshotEntity(id = 1, issuerId = 1, publicationEpochDay = day(1), totalShortPercent = 1.0, holderCount = 1, fetchedAtEpochMillis = 0),
            PositionSnapshotEntity(id = 2, issuerId = 1, publicationEpochDay = day(3), totalShortPercent = 1.5, holderCount = 2, fetchedAtEpochMillis = 0),
        )

        val summaries = buildSummaries(issuers, snapshots)

        assertEquals(1, summaries.size)
        val summary = summaries.getValue(1)
        assertEquals(1.5, summary.totalShortPercent, 0.0001)
        assertEquals(1.0, summary.previousTotalShortPercent!!, 0.0001)
        assertEquals(0.5, summary.changePercentagePoints!!, 0.0001)
        assertNull(summaries[2])
    }

    @Test
    fun `buildHistory sorts ascending and computes delta vs previous entry`() {
        val snapshots = listOf(
            PositionSnapshotEntity(id = 2, issuerId = 1, publicationEpochDay = day(5), totalShortPercent = 2.0, holderCount = 1, fetchedAtEpochMillis = 0),
            PositionSnapshotEntity(id = 1, issuerId = 1, publicationEpochDay = day(1), totalShortPercent = 1.0, holderCount = 1, fetchedAtEpochMillis = 0),
        )

        val history = buildHistory(snapshots)

        assertEquals(2, history.size)
        assertEquals(day(1), history[0].date.toEpochDay())
        assertNull(history[0].changeVsPrevious)
        assertEquals(1.0, history[1].changeVsPrevious!!, 0.0001)
    }

    @Test
    fun `buildLatestHolderPositions keeps only the newest entry per holder with delta`() {
        val holders = listOf(
            HolderPositionEntity(id = 1, issuerId = 1, holderName = "Fundusz X", percent = 0.6, positionEpochDay = day(1)),
            HolderPositionEntity(id = 2, issuerId = 1, holderName = "Fundusz X", percent = 0.9, positionEpochDay = day(5)),
        )

        val result = buildLatestHolderPositions(holders)

        assertEquals(1, result.size)
        assertEquals(0.9, result.first().percent, 0.0001)
        assertEquals(0.6, result.first().previousPercent!!, 0.0001)
    }

    private fun day(offset: Long): Long = LocalDate.of(2024, 1, 1).plusDays(offset).toEpochDay()
}
