package pl.mlorek.gpwshort.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pl.mlorek.gpwshort.data.local.dao.HolderPositionDao
import pl.mlorek.gpwshort.data.local.dao.IssuerDao
import pl.mlorek.gpwshort.data.local.dao.PositionSnapshotDao
import pl.mlorek.gpwshort.data.local.dao.SyncMetaDao
import pl.mlorek.gpwshort.data.local.dao.WatchlistDao
import pl.mlorek.gpwshort.data.local.entity.HolderPositionEntity
import pl.mlorek.gpwshort.data.local.entity.IssuerEntity
import pl.mlorek.gpwshort.data.local.entity.PositionSnapshotEntity
import pl.mlorek.gpwshort.data.local.entity.SyncMetaEntity
import pl.mlorek.gpwshort.data.local.entity.WatchlistEntity
import pl.mlorek.gpwshort.data.mapper.buildHistory
import pl.mlorek.gpwshort.data.mapper.buildLatestHolderPositions
import pl.mlorek.gpwshort.data.mapper.buildSummaries
import pl.mlorek.gpwshort.data.mapper.epochDayToLocalDate
import pl.mlorek.gpwshort.data.mapper.toDomain
import pl.mlorek.gpwshort.data.remote.KnfRemoteDataSource
import pl.mlorek.gpwshort.data.remote.dto.KnfParseResult
import pl.mlorek.gpwshort.data.remote.dto.SignificantPositionRow
import pl.mlorek.gpwshort.data.remote.parser.ParsingException
import pl.mlorek.gpwshort.di.IoDispatcher
import pl.mlorek.gpwshort.domain.model.IssuerDetail
import pl.mlorek.gpwshort.domain.model.IssuerSummary
import pl.mlorek.gpwshort.domain.model.WatchlistItem
import pl.mlorek.gpwshort.domain.repository.ShortPositionsRepository
import pl.mlorek.gpwshort.util.AppResult
import pl.mlorek.gpwshort.util.ErrorType
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class ShortPositionsRepositoryImpl @Inject constructor(
    private val remoteDataSource: KnfRemoteDataSource,
    private val issuerDao: IssuerDao,
    private val snapshotDao: PositionSnapshotDao,
    private val holderDao: HolderPositionDao,
    private val watchlistDao: WatchlistDao,
    private val syncMetaDao: SyncMetaDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ShortPositionsRepository {

    override fun observeIssuerSummaries(): Flow<List<IssuerSummary>> =
        combine(issuerDao.observeAll(), snapshotDao.observeAllDescending()) { issuers, snapshots ->
            buildSummaries(issuers, snapshots).values.filterNot { it.isClosed }
        }.flowOn(ioDispatcher)

    override fun observeIssuerDetail(issuerId: Long): Flow<IssuerDetail?> =
        combine(
            issuerDao.observeById(issuerId),
            snapshotDao.observeForIssuer(issuerId),
            holderDao.observeForIssuer(issuerId),
        ) { issuer, snapshots, holders ->
            if (issuer == null) return@combine null
            IssuerDetail(
                issuer = issuer.toDomain(),
                isClosed = issuer.isClosed,
                closedSince = if (issuer.isClosed) epochDayToLocalDate(issuer.lastOpenEpochDay) else null,
                fullHistory = buildHistory(snapshots),
                holders = buildLatestHolderPositions(holders),
            )
        }.flowOn(ioDispatcher)

    override fun observeLastPublicationDate(): Flow<LocalDate?> =
        syncMetaDao.observe().map { meta -> meta?.let { epochDayToLocalDate(it.lastPublicationEpochDay) } }
            .flowOn(ioDispatcher)

    override fun observeWatchlist(): Flow<List<WatchlistItem>> =
        combine(
            watchlistDao.observeAll(),
            issuerDao.observeAll(),
            snapshotDao.observeAllDescending(),
        ) { watchlist, issuers, snapshots ->
            val summaries = buildSummaries(issuers, snapshots)
            val issuersById = issuers.associateBy { it.id }
            watchlist.mapNotNull { entry ->
                val issuer = summaries[entry.issuerId]?.issuer
                    ?: issuersById[entry.issuerId]?.toDomain()
                    ?: return@mapNotNull null
                WatchlistItem(
                    issuer = issuer,
                    alertThresholdPercentagePoints = entry.thresholdPercentagePoints,
                    latestSummary = summaries[entry.issuerId],
                )
            }
        }.flowOn(ioDispatcher)

    override suspend fun addToWatchlist(issuerId: Long, thresholdPercentagePoints: Double) {
        withContext(ioDispatcher) {
            watchlistDao.upsert(
                WatchlistEntity(
                    issuerId = issuerId,
                    thresholdPercentagePoints = thresholdPercentagePoints,
                    addedAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    override suspend fun removeFromWatchlist(issuerId: Long) {
        withContext(ioDispatcher) { watchlistDao.delete(issuerId) }
    }

    override suspend fun lastSuccessfulSyncAt(): LocalDateTime? = withContext(ioDispatcher) {
        syncMetaDao.get()?.let {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it.lastSuccessEpochMillis), ZoneId.systemDefault())
        }
    }

    override suspend fun refresh(): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val parseResult = remoteDataSource.fetchLatest()
            importParseResult(parseResult)
            AppResult.Success(Unit)
        } catch (e: ParsingException) {
            AppResult.Error(ErrorType.PARSING, e)
        } catch (e: IOException) {
            AppResult.Error(ErrorType.NETWORK, e)
        } catch (e: Exception) {
            AppResult.Error(ErrorType.UNKNOWN, e)
        }
    }

    private suspend fun importParseResult(parseResult: KnfParseResult) {
        val now = System.currentTimeMillis()
        val publicationDate =
            parseResult.summaryPositions.maxOfOrNull { it.positionDate }
                ?: parseResult.significantPositions.maxOfOrNull { it.positionDate }
                ?: LocalDate.now()
        val publicationEpochDay = publicationDate.toEpochDay()

        val holderRowsByIssuerKey: Map<String, List<SignificantPositionRow>> =
            parseResult.significantPositions.groupBy { normalizeIssuerKey(it.issuerName, it.isin) }
        val handledKeys = mutableSetOf<String>()
        val openIssuerIds = mutableSetOf<Long>()

        for (summaryRow in parseResult.summaryPositions) {
            val key = normalizeIssuerKey(summaryRow.issuerName, summaryRow.isin)
            handledKeys += key
            val issuerEntity = findOrCreateIssuer(summaryRow.issuerName, summaryRow.isin, publicationEpochDay)
            openIssuerIds += issuerEntity.id

            val matchingHolders = holderRowsByIssuerKey[key].orEmpty()
            importIssuerBatch(
                issuerEntity = issuerEntity,
                publicationEpochDay = publicationEpochDay,
                totalPercent = summaryRow.percent,
                holderRows = matchingHolders,
                fetchedAt = now,
            )
        }

        // Emitenci obecni tylko w tabeli znaczących pozycji (bez odpowiadającego wiersza
        // sumarycznego) - nie powinno się zdarzać przy spójnych danych KNF, ale traktujemy
        // to defensywnie: liczymy sumę z dostępnych pozycji zamiast gubić dane.
        for ((key, rows) in holderRowsByIssuerKey) {
            if (key in handledKeys) continue
            val first = rows.first()
            val issuerEntity = findOrCreateIssuer(first.issuerName, first.isin, publicationEpochDay)
            openIssuerIds += issuerEntity.id
            importIssuerBatch(
                issuerEntity = issuerEntity,
                publicationEpochDay = publicationEpochDay,
                totalPercent = rows.sumOf { it.percent },
                holderRows = rows,
                fetchedAt = now,
            )
        }

        val openIds = openIssuerIds.toList()
        issuerDao.markMissingAsClosed(openIds)
        issuerDao.reopen(openIds)

        syncMetaDao.upsert(
            SyncMetaEntity(lastSuccessEpochMillis = now, lastPublicationEpochDay = publicationEpochDay),
        )
    }

    private suspend fun importIssuerBatch(
        issuerEntity: IssuerEntity,
        publicationEpochDay: Long,
        totalPercent: Double,
        holderRows: List<SignificantPositionRow>,
        fetchedAt: Long,
    ) {
        snapshotDao.upsert(
            PositionSnapshotEntity(
                issuerId = issuerEntity.id,
                publicationEpochDay = publicationEpochDay,
                totalShortPercent = totalPercent,
                holderCount = holderRows.map { it.holderName }.distinct().size,
                fetchedAtEpochMillis = fetchedAt,
            ),
        )
        if (holderRows.isNotEmpty()) {
            holderDao.insertAll(
                holderRows.map { row ->
                    HolderPositionEntity(
                        issuerId = issuerEntity.id,
                        holderName = row.holderName,
                        percent = row.percent,
                        positionEpochDay = row.positionDate.toEpochDay(),
                    )
                },
            )
        }
    }

    private suspend fun findOrCreateIssuer(name: String, isin: String?, seenEpochDay: Long): IssuerEntity {
        val existing = issuerDao.findByName(name)
        if (existing == null) {
            val id = issuerDao.insert(IssuerEntity(name = name, isin = isin, lastOpenEpochDay = seenEpochDay))
            return IssuerEntity(id = id, name = name, isin = isin, lastOpenEpochDay = seenEpochDay)
        }
        val updated = existing.copy(
            isin = isin ?: existing.isin,
            lastOpenEpochDay = maxOf(existing.lastOpenEpochDay, seenEpochDay),
        )
        if (updated != existing) issuerDao.update(updated)
        return updated
    }

    private fun normalizeIssuerKey(name: String, isin: String?): String =
        isin?.takeIf { it.isNotBlank() } ?: name.trim().lowercase()
}
