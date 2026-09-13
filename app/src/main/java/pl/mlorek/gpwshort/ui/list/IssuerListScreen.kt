package pl.mlorek.gpwshort.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.mlorek.gpwshort.R
import pl.mlorek.gpwshort.domain.model.IssuerSummary
import pl.mlorek.gpwshort.domain.model.SortOrder
import pl.mlorek.gpwshort.ui.components.ChangeIndicator
import pl.mlorek.gpwshort.ui.components.ErrorView
import pl.mlorek.gpwshort.ui.components.ShimmerListPlaceholder
import pl.mlorek.gpwshort.ui.components.messageFor
import pl.mlorek.gpwshort.util.formatDate
import pl.mlorek.gpwshort.util.formatPercent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssuerListScreen(
    onIssuerClick: (Long) -> Unit,
    viewModel: IssuerListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = {}) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LastUpdateBanner(
                date = uiState.lastPublicationDate,
                isStale = uiState.isStale,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.list_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )
            SortRow(
                selected = uiState.sortOrder,
                onSelect = viewModel::onSortOrderChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Box(modifier = Modifier.fillMaxSize()) {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh(force = true) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when {
                        uiState.summaries.isEmpty() && uiState.isRefreshing ->
                            ShimmerListPlaceholder()

                        uiState.summaries.isEmpty() && uiState.error != null ->
                            ErrorView(message = messageFor(uiState.error!!), onRetry = viewModel::retry)

                        uiState.summaries.isEmpty() ->
                            Text(
                                text = stringResource(R.string.list_empty),
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )

                        else -> LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(uiState.summaries, key = { it.issuer.id }) { summary ->
                                IssuerListItem(summary = summary, onClick = { onIssuerClick(summary.issuer.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LastUpdateBanner(date: java.time.LocalDate?, isStale: Boolean, modifier: Modifier = Modifier) {
    if (date == null) return
    val text = if (isStale) {
        stringResource(R.string.list_last_update_stale, formatDate(date))
    } else {
        stringResource(R.string.list_last_update, formatDate(date))
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = if (isStale) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun SortRow(selected: SortOrder, onSelect: (SortOrder) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selected == SortOrder.TOTAL_PERCENT_DESC,
                onClick = { onSelect(SortOrder.TOTAL_PERCENT_DESC) },
                label = { Text(stringResource(R.string.list_sort_percent)) },
            )
            FilterChip(
                selected = selected == SortOrder.CHANGE_DESC,
                onClick = { onSelect(SortOrder.CHANGE_DESC) },
                label = { Text(stringResource(R.string.list_sort_change)) },
            )
            FilterChip(
                selected = selected == SortOrder.ALPHABETICAL,
                onClick = { onSelect(SortOrder.ALPHABETICAL) },
                label = { Text(stringResource(R.string.list_sort_alpha)) },
            )
        }
    }
}

@Composable
private fun IssuerListItem(summary: IssuerSummary, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.issuer.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                androidx.compose.foundation.layout.Row {
                    Text(
                        text = summary.issuer.isin ?: "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "  •  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val holdersText = if (summary.holderCount == 1) {
                        stringResource(R.string.list_holder_count_single)
                    } else {
                        stringResource(R.string.list_holders_count, summary.holderCount)
                    }
                    Text(
                        text = holdersText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatPercent(summary.totalShortPercent),
                    style = MaterialTheme.typography.titleMedium,
                )
                ChangeIndicator(
                    direction = summary.changeDirection,
                    changePercentagePoints = summary.changePercentagePoints,
                )
            }
        }
    }
}
