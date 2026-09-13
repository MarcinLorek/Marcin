package pl.mlorek.gpwshort.domain.usecase

import pl.mlorek.gpwshort.domain.repository.ShortPositionsRepository
import pl.mlorek.gpwshort.util.AppResult
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class RefreshDataUseCase @Inject constructor(
    private val repository: ShortPositionsRepository,
) {
    suspend operator fun invoke(force: Boolean = false, staleAfterHours: Long = 12): AppResult<Unit> {
        if (!force) {
            val lastSync = repository.lastSuccessfulSyncAt()
            if (lastSync != null && ChronoUnit.HOURS.between(lastSync, LocalDateTime.now()) < staleAfterHours) {
                return AppResult.Success(Unit)
            }
        }
        return repository.refresh()
    }
}
