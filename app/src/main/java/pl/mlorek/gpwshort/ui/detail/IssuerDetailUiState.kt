package pl.mlorek.gpwshort.ui.detail

import pl.mlorek.gpwshort.domain.model.DateRange
import pl.mlorek.gpwshort.domain.model.IssuerDetail
import pl.mlorek.gpwshort.domain.model.TrendSummary
import pl.mlorek.gpwshort.util.ErrorType

data class IssuerDetailUiState(
    val detail: IssuerDetail? = null,
    val range: DateRange = DateRange.DAYS_90,
    val trend: TrendSummary? = null,
    val isWatched: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: ErrorType? = null,
)
