package pl.mlorek.gpwshort.domain.repository

import kotlinx.coroutines.flow.Flow
import pl.mlorek.gpwshort.domain.model.IssuerDetail
import pl.mlorek.gpwshort.domain.model.IssuerSummary
import pl.mlorek.gpwshort.domain.model.WatchlistItem
import pl.mlorek.gpwshort.util.AppResult
import java.time.LocalDate
import java.time.LocalDateTime

interface ShortPositionsRepository {

    fun observeIssuerSummaries(): Flow<List<IssuerSummary>>

    fun observeIssuerDetail(issuerId: Long): Flow<IssuerDetail?>

    /** Data publikacji przez KNF ostatniego zapisanego lokalnie snapshotu (nie data pobrania). */
    fun observeLastPublicationDate(): Flow<LocalDate?>

    fun observeWatchlist(): Flow<List<WatchlistItem>>

    suspend fun addToWatchlist(issuerId: Long, thresholdPercentagePoints: Double)

    suspend fun removeFromWatchlist(issuerId: Long)

    suspend fun lastSuccessfulSyncAt(): LocalDateTime?

    /** Pobiera świeże dane z KNF, dedupikuje i zapisuje nowy snapshot w Room. */
    suspend fun refresh(): AppResult<Unit>
}
