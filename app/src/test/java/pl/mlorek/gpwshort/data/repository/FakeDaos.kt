package pl.mlorek.gpwshort.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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

/** Proste, w pełni w pamięci implementacje DAO na potrzeby testów repozytorium (bez Room/Robolectric). */

class FakeIssuerDao : IssuerDao {
    private val state = MutableStateFlow<List<IssuerEntity>>(emptyList())
    private var nextId = 1L
    val current: List<IssuerEntity> get() = state.value

    override fun observeAll(): StateFlow<List<IssuerEntity>> = state
    override fun observeById(id: Long) = state.map { list -> list.find { it.id == id } }

    override suspend fun findByName(name: String): IssuerEntity? = state.value.find { it.name == name }

    override suspend fun insert(issuer: IssuerEntity): Long {
        val id = nextId++
        state.value = state.value + issuer.copy(id = id)
        return id
    }

    override suspend fun update(issuer: IssuerEntity) {
        state.value = state.value.map { if (it.id == issuer.id) issuer else it }
    }

    override suspend fun reopen(ids: List<Long>) {
        state.value = state.value.map { if (it.id in ids) it.copy(isClosed = false) else it }
    }

    override suspend fun markMissingAsClosed(openIssuerIds: List<Long>) {
        state.value = state.value.map { if (it.id !in openIssuerIds) it.copy(isClosed = true) else it }
    }

    override suspend fun allIds(): List<Long> = state.value.map { it.id }
}

class FakePositionSnapshotDao : PositionSnapshotDao {
    private val state = MutableStateFlow<List<PositionSnapshotEntity>>(emptyList())
    private var nextId = 1L
    val current: List<PositionSnapshotEntity> get() = state.value

    override suspend fun upsert(snapshot: PositionSnapshotEntity): Long {
        val existing = state.value.find {
            it.issuerId == snapshot.issuerId && it.publicationEpochDay == snapshot.publicationEpochDay
        }
        return if (existing != null) {
            val updated = snapshot.copy(id = existing.id)
            state.value = state.value.map { if (it.id == existing.id) updated else it }
            existing.id
        } else {
            val id = nextId++
            state.value = state.value + snapshot.copy(id = id)
            id
        }
    }

    override fun observeAllDescending(): StateFlow<List<PositionSnapshotEntity>> = state

    override fun observeForIssuer(issuerId: Long) =
        state.map { list -> list.filter { it.issuerId == issuerId } }

    override suspend fun latestPublicationEpochDay(): Long? = state.value.maxOfOrNull { it.publicationEpochDay }

    override suspend fun issuerIdsForPublicationDay(epochDay: Long): List<Long> =
        state.value.filter { it.publicationEpochDay == epochDay }.map { it.issuerId }.distinct()

    override suspend fun findExact(issuerId: Long, epochDay: Long): PositionSnapshotEntity? =
        state.value.find { it.issuerId == issuerId && it.publicationEpochDay == epochDay }

    override suspend fun latestForIssuer(issuerId: Long): PositionSnapshotEntity? =
        state.value.filter { it.issuerId == issuerId }.maxByOrNull { it.publicationEpochDay }
}

class FakeHolderPositionDao : HolderPositionDao {
    private val state = MutableStateFlow<List<HolderPositionEntity>>(emptyList())
    private var nextId = 1L
    val current: List<HolderPositionEntity> get() = state.value

    override suspend fun insertAll(holders: List<HolderPositionEntity>): List<Long> {
        val ids = mutableListOf<Long>()
        for (holder in holders) {
            val duplicate = state.value.any {
                it.issuerId == holder.issuerId &&
                    it.holderName == holder.holderName &&
                    it.positionEpochDay == holder.positionEpochDay &&
                    it.percent == holder.percent
            }
            if (duplicate) {
                ids += -1
            } else {
                val id = nextId++
                state.value = state.value + holder.copy(id = id)
                ids += id
            }
        }
        return ids
    }

    override fun observeForIssuer(issuerId: Long) =
        state.map { list -> list.filter { it.issuerId == issuerId } }
}

class FakeWatchlistDao : WatchlistDao {
    private val state = MutableStateFlow<List<WatchlistEntity>>(emptyList())

    override fun observeAll(): StateFlow<List<WatchlistEntity>> = state

    override suspend fun find(issuerId: Long): WatchlistEntity? = state.value.find { it.issuerId == issuerId }

    override suspend fun upsert(item: WatchlistEntity) {
        state.value = state.value.filterNot { it.issuerId == item.issuerId } + item
    }

    override suspend fun delete(issuerId: Long) {
        state.value = state.value.filterNot { it.issuerId == issuerId }
    }

    override suspend fun markNotified(issuerId: Long, snapshotId: Long) {
        state.value = state.value.map {
            if (it.issuerId == issuerId) it.copy(lastNotifiedSnapshotId = snapshotId) else it
        }
    }
}

class FakeSyncMetaDao : SyncMetaDao {
    private val state = MutableStateFlow<SyncMetaEntity?>(null)

    override suspend fun get(): SyncMetaEntity? = state.value

    override fun observe(): StateFlow<SyncMetaEntity?> = state

    override suspend fun upsert(meta: SyncMetaEntity) {
        state.value = meta
    }
}
