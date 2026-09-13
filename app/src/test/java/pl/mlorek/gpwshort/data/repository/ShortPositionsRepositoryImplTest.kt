package pl.mlorek.gpwshort.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.mlorek.gpwshort.data.remote.KnfRemoteDataSource
import pl.mlorek.gpwshort.data.remote.dto.KnfParseResult
import pl.mlorek.gpwshort.data.remote.dto.SignificantPositionRow
import pl.mlorek.gpwshort.data.remote.dto.SummaryPositionRow
import pl.mlorek.gpwshort.util.AppResult
import pl.mlorek.gpwshort.util.ErrorType
import java.io.IOException
import java.time.LocalDate

class ShortPositionsRepositoryImplTest {

    private val remote = mockk<KnfRemoteDataSource>()
    private val issuerDao = FakeIssuerDao()
    private val snapshotDao = FakePositionSnapshotDao()
    private val holderDao = FakeHolderPositionDao()
    private val watchlistDao = FakeWatchlistDao()
    private val syncMetaDao = FakeSyncMetaDao()
    private val dispatcher = StandardTestDispatcher()

    private val repository = ShortPositionsRepositoryImpl(
        remoteDataSource = remote,
        issuerDao = issuerDao,
        snapshotDao = snapshotDao,
        holderDao = holderDao,
        watchlistDao = watchlistDao,
        syncMetaDao = syncMetaDao,
        ioDispatcher = dispatcher,
    )

    @Test
    fun `refresh imports summary and significant rows and derives holder count`() = runTest(dispatcher) {
        coEvery { remote.fetchLatest() } returns KnfParseResult(
            significantPositions = listOf(
                SignificantPositionRow("Fundusz A", "Spółka SA", "PLXXX00010", 0.6, LocalDate.of(2024, 1, 10)),
                SignificantPositionRow("Fundusz B", "Spółka SA", "PLXXX00010", 0.7, LocalDate.of(2024, 1, 10)),
            ),
            summaryPositions = listOf(
                SummaryPositionRow("Spółka SA", "PLXXX00010", 1.4, LocalDate.of(2024, 1, 10)),
            ),
        )

        val result = repository.refresh()

        assertTrue(result is AppResult.Success)
        assertEquals(1, issuerDao.current.size)
        assertEquals("Spółka SA", issuerDao.current.first().name)
        assertEquals(1, snapshotDao.current.size)
        assertEquals(1.4, snapshotDao.current.first().totalShortPercent, 0.0001)
        assertEquals(2, snapshotDao.current.first().holderCount)
        assertEquals(2, holderDao.current.size)
    }

    @Test
    fun `issuer missing from a later refresh is marked closed but history is kept`() = runTest(dispatcher) {
        coEvery { remote.fetchLatest() } returnsMany listOf(
            KnfParseResult(emptyList(), listOf(SummaryPositionRow("Spółka SA", null, 1.0, LocalDate.of(2024, 1, 1)))),
            KnfParseResult(emptyList(), listOf(SummaryPositionRow("Inna Spółka", null, 2.0, LocalDate.of(2024, 1, 2)))),
        )

        repository.refresh()
        repository.refresh()

        val closedIssuer = issuerDao.current.first { it.name == "Spółka SA" }
        assertTrue(closedIssuer.isClosed)
        assertEquals(2, issuerDao.current.size)
        assertEquals(2, snapshotDao.current.size)
    }

    @Test
    fun `repeated identical refresh does not duplicate holder or snapshot rows`() = runTest(dispatcher) {
        val parseResult = KnfParseResult(
            significantPositions = listOf(
                SignificantPositionRow("Fundusz A", "Spółka SA", null, 0.6, LocalDate.of(2024, 1, 10)),
            ),
            summaryPositions = listOf(SummaryPositionRow("Spółka SA", null, 0.6, LocalDate.of(2024, 1, 10))),
        )
        coEvery { remote.fetchLatest() } returns parseResult

        repository.refresh()
        repository.refresh()

        assertEquals(1, holderDao.current.size)
        assertEquals(1, snapshotDao.current.size)
    }

    @Test
    fun `network failure maps to AppResult Error with NETWORK type`() = runTest(dispatcher) {
        coEvery { remote.fetchLatest() } throws IOException("boom")

        val result = repository.refresh()

        assertTrue(result is AppResult.Error)
        assertEquals(ErrorType.NETWORK, (result as AppResult.Error).type)
    }
}
