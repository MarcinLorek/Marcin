package pl.mlorek.gpwshort.domain.usecase

import pl.mlorek.gpwshort.domain.repository.ShortPositionsRepository
import javax.inject.Inject

class ToggleWatchlistUseCase @Inject constructor(
    private val repository: ShortPositionsRepository,
) {
    suspend fun add(issuerId: Long, thresholdPercentagePoints: Double) =
        repository.addToWatchlist(issuerId, thresholdPercentagePoints)

    suspend fun remove(issuerId: Long) = repository.removeFromWatchlist(issuerId)
}
