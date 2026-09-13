package pl.mlorek.gpwshort.domain.usecase

import kotlinx.coroutines.flow.Flow
import pl.mlorek.gpwshort.domain.repository.ShortPositionsRepository
import java.time.LocalDate
import javax.inject.Inject

class GetLastPublicationDateUseCase @Inject constructor(
    private val repository: ShortPositionsRepository,
) {
    operator fun invoke(): Flow<LocalDate?> = repository.observeLastPublicationDate()
}
