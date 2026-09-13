package pl.mlorek.gpwshort.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.mlorek.gpwshort.domain.model.SortOrder
import pl.mlorek.gpwshort.domain.usecase.GetIssuerSummariesUseCase
import pl.mlorek.gpwshort.domain.usecase.GetLastPublicationDateUseCase
import pl.mlorek.gpwshort.domain.usecase.RefreshDataUseCase
import pl.mlorek.gpwshort.util.AppResult
import javax.inject.Inject

@HiltViewModel
class IssuerListViewModel @Inject constructor(
    private val getIssuerSummaries: GetIssuerSummariesUseCase,
    private val getLastPublicationDate: GetLastPublicationDateUseCase,
    private val refreshData: RefreshDataUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val sortOrder = MutableStateFlow(SortOrder.TOTAL_PERCENT_DESC)
    private val isRefreshing = MutableStateFlow(true)
    private val lastError = MutableStateFlow<pl.mlorek.gpwshort.util.ErrorType?>(null)

    val uiState: StateFlow<IssuerListUiState> = combine(
        combine(query, sortOrder) { q, s -> q to s }
            .flatMapLatest { (q, s) -> getIssuerSummaries(q, s) },
        getLastPublicationDate(),
        isRefreshing,
        lastError,
    ) { summaries, lastDate, refreshing, error ->
        IssuerListUiState(
            summaries = summaries,
            query = query.value,
            sortOrder = sortOrder.value,
            isRefreshing = refreshing,
            lastPublicationDate = lastDate,
            error = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IssuerListUiState())

    init {
        refresh(force = false)
    }

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun onSortOrderChange(newSortOrder: SortOrder) {
        sortOrder.value = newSortOrder
    }

    fun retry() = refresh(force = true)

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            isRefreshing.value = true
            when (val result = refreshData(force = force)) {
                is AppResult.Success -> lastError.value = null
                is AppResult.Error -> lastError.value = result.type
            }
            isRefreshing.value = false
        }
    }
}
