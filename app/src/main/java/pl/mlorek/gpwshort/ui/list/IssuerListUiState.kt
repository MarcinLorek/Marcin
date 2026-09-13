package pl.mlorek.gpwshort.ui.list

import pl.mlorek.gpwshort.domain.model.IssuerSummary
import pl.mlorek.gpwshort.domain.model.SortOrder
import pl.mlorek.gpwshort.util.ErrorType
import java.time.LocalDate

data class IssuerListUiState(
    val summaries: List<IssuerSummary> = emptyList(),
    val query: String = "",
    val sortOrder: SortOrder = SortOrder.TOTAL_PERCENT_DESC,
    val isRefreshing: Boolean = true,
    val lastPublicationDate: LocalDate? = null,
    val error: ErrorType? = null,
) {
    /** Dane wyświetlane pochodzą sprzed nieudanej próby odświeżenia z powodu sieci. */
    val isStale: Boolean
        get() = error == ErrorType.NETWORK && lastPublicationDate != null
}
