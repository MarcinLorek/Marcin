package pl.mlorek.gpwshort.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import pl.mlorek.gpwshort.domain.model.WatchlistItem
import pl.mlorek.gpwshort.domain.usecase.GetWatchlistUseCase
import javax.inject.Inject

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    getWatchlist: GetWatchlistUseCase,
) : ViewModel() {

    val uiState: StateFlow<List<WatchlistItem>> = getWatchlist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
