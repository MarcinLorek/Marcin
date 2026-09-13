package pl.mlorek.gpwshort.domain.usecase

import kotlinx.coroutines.flow.Flow
import pl.mlorek.gpwshort.domain.model.WatchlistItem
import pl.mlorek.gpwshort.domain.repository.ShortPositionsRepository
import javax.inject.Inject

class GetWatchlistUseCase @Inject constructor(
    private val repository: ShortPositionsRepository,
) {
    operator fun invoke(): Flow<List<WatchlistItem>> = repository.observeWatchlist()
}
