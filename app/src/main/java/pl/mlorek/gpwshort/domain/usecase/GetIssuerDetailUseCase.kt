package pl.mlorek.gpwshort.domain.usecase

import kotlinx.coroutines.flow.Flow
import pl.mlorek.gpwshort.domain.model.IssuerDetail
import pl.mlorek.gpwshort.domain.repository.ShortPositionsRepository
import javax.inject.Inject

class GetIssuerDetailUseCase @Inject constructor(
    private val repository: ShortPositionsRepository,
) {
    operator fun invoke(issuerId: Long): Flow<IssuerDetail?> = repository.observeIssuerDetail(issuerId)
}
