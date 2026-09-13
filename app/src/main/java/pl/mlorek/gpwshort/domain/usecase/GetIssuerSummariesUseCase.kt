package pl.mlorek.gpwshort.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.mlorek.gpwshort.domain.model.IssuerSummary
import pl.mlorek.gpwshort.domain.model.SortOrder
import pl.mlorek.gpwshort.domain.repository.ShortPositionsRepository
import javax.inject.Inject

class GetIssuerSummariesUseCase @Inject constructor(
    private val repository: ShortPositionsRepository,
) {
    operator fun invoke(query: String, sortOrder: SortOrder): Flow<List<IssuerSummary>> =
        repository.observeIssuerSummaries().map { summaries ->
            summaries
                .filter { query.isBlank() || it.issuer.name.contains(query, ignoreCase = true) }
                .sortedWith(comparatorFor(sortOrder))
        }

    private fun comparatorFor(sortOrder: SortOrder): Comparator<IssuerSummary> = when (sortOrder) {
        SortOrder.TOTAL_PERCENT_DESC -> compareByDescending { it.totalShortPercent }
        SortOrder.CHANGE_DESC -> compareByDescending { kotlin.math.abs(it.changePercentagePoints ?: 0.0) }
        SortOrder.ALPHABETICAL -> compareBy { it.issuer.name.lowercase() }
    }
}
