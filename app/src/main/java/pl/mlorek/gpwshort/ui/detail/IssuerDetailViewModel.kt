package pl.mlorek.gpwshort.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.mlorek.gpwshort.domain.model.DateRange
import pl.mlorek.gpwshort.domain.usecase.GetIssuerDetailUseCase
import pl.mlorek.gpwshort.domain.usecase.GetWatchlistUseCase
import pl.mlorek.gpwshort.domain.usecase.RefreshDataUseCase
import pl.mlorek.gpwshort.domain.usecase.SummarizeTrendUseCase
import pl.mlorek.gpwshort.domain.usecase.ToggleWatchlistUseCase
import pl.mlorek.gpwshort.ui.navigation.Destinations
import pl.mlorek.gpwshort.util.AppResult
import pl.mlorek.gpwshort.util.ErrorType
import javax.inject.Inject

@HiltViewModel
class IssuerDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getIssuerDetail: GetIssuerDetailUseCase,
    private val summarizeTrend: SummarizeTrendUseCase,
    private val refreshData: RefreshDataUseCase,
    private val getWatchlist: GetWatchlistUseCase,
    private val toggleWatchlist: ToggleWatchlistUseCase,
) : ViewModel() {

    private val issuerId: Long = checkNotNull(savedStateHandle[Destinations.DETAIL_ARG_ISSUER_ID])

    private val range = MutableStateFlow(DateRange.DAYS_90)
    private val isRefreshing = MutableStateFlow(false)
    private val lastError = MutableStateFlow<ErrorType?>(null)

    val uiState: StateFlow<IssuerDetailUiState> = combine(
        getIssuerDetail(issuerId),
        range,
        getWatchlist(),
        isRefreshing,
        lastError,
    ) { detail, currentRange, watchlist, refreshing, error ->
        IssuerDetailUiState(
            detail = detail,
            range = currentRange,
            trend = detail?.let { summarizeTrend(it.historyFor(currentRange)) },
            isWatched = watchlist.any { it.issuer.id == issuerId },
            isRefreshing = refreshing,
            error = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IssuerDetailUiState())

    fun onRangeChange(newRange: DateRange) {
        range.value = newRange
    }

    fun retry() = refresh()

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            when (val result = refreshData(force = true)) {
                is AppResult.Success -> lastError.value = null
                is AppResult.Error -> lastError.value = result.type
            }
            isRefreshing.value = false
        }
    }

    fun toggleWatch(defaultThresholdPercentagePoints: Double = 0.5) {
        viewModelScope.launch {
            if (uiState.value.isWatched) {
                toggleWatchlist.remove(issuerId)
            } else {
                toggleWatchlist.add(issuerId, defaultThresholdPercentagePoints)
            }
        }
    }
}
