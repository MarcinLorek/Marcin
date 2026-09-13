package pl.mlorek.gpwshort.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.mlorek.gpwshort.R
import pl.mlorek.gpwshort.domain.model.DateRange
import pl.mlorek.gpwshort.domain.model.HolderPosition
import pl.mlorek.gpwshort.domain.model.IssuerDetail
import pl.mlorek.gpwshort.domain.model.PositionHistoryPoint
import pl.mlorek.gpwshort.domain.model.TrendSummary
import pl.mlorek.gpwshort.ui.components.ChangeIndicator
import pl.mlorek.gpwshort.ui.components.ErrorView
import pl.mlorek.gpwshort.ui.components.PositionHistoryChart
import pl.mlorek.gpwshort.ui.components.PullToRefreshBox
import pl.mlorek.gpwshort.ui.components.ShimmerListPlaceholder
import pl.mlorek.gpwshort.ui.components.messageFor
import pl.mlorek.gpwshort.util.formatDate
import pl.mlorek.gpwshort.util.formatPercent
import pl.mlorek.gpwshort.util.formatSignedPercentagePoints
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssuerDetailScreen(
    onBack: () -> Unit,
    viewModel: IssuerDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.detail?.issuer?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (uiState.detail != null) {
                        IconButton(onClick = { viewModel.toggleWatch() }) {
                            Icon(
                                imageVector = if (uiState.isWatched) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = stringResource(
                                    if (uiState.isWatched) R.string.detail_remove_watchlist else R.string.detail_add_watchlist,
                                ),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                val detail = uiState.detail
                when {
                    detail == null && uiState.error != null ->
                        ErrorView(message = messageFor(uiState.error!!), onRetry = viewModel::retry)

                    detail == null -> ShimmerListPlaceholder()

                    else -> DetailContent(
                        detail = detail,
                        state = uiState,
                        onRangeChange = viewModel::onRangeChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailContent(
    detail: IssuerDetail,
    state: IssuerDetailUiState,
    onRangeChange: (DateRange) -> Unit,
) {
    val filteredHistory = detail.historyFor(state.range)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (detail.isClosed) {
            item { ClosedBanner(closedSince = detail.closedSince) }
        }
        item {
            Text(
                text = detail.issuer.isin?.let { stringResource(R.string.detail_isin, it) } ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { RangeSelector(selected = state.range, onSelect = onRangeChange) }
        item { PositionHistoryChart(points = filteredHistory, modifier = Modifier.fillMaxWidth()) }
        item { TrendCard(trend = state.trend) }
        item {
            Text(stringResource(R.string.detail_history_header), style = MaterialTheme.typography.titleMedium)
        }
        items(filteredHistory.asReversed()) { point -> HistoryRow(point) }
        item {
            Text(stringResource(R.string.detail_holders_header), style = MaterialTheme.typography.titleMedium)
        }
        items(detail.holders) { holder -> HolderRow(holder) }
    }
}

@Composable
private fun ClosedBanner(closedSince: LocalDate?) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.detail_closed_position),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (closedSince != null) {
                Text(
                    text = stringResource(R.string.detail_closed_since, formatDate(closedSince)),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun RangeSelector(selected: DateRange, onSelect: (DateRange) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DateRange.entries.forEach { range ->
            FilterChip(
                selected = selected == range,
                onClick = { onSelect(range) },
                label = { Text(labelFor(range)) },
            )
        }
    }
}

@Composable
private fun labelFor(range: DateRange): String = when (range) {
    DateRange.DAYS_30 -> stringResource(R.string.detail_range_30d)
    DateRange.DAYS_90 -> stringResource(R.string.detail_range_90d)
    DateRange.YEAR_1 -> stringResource(R.string.detail_range_1y)
    DateRange.MAX -> stringResource(R.string.detail_range_max)
}

@Composable
private fun TrendCard(trend: TrendSummary?) {
    if (trend == null) return
    val (icon, text) = when (trend) {
        is TrendSummary.Rising -> Icons.Filled.TrendingUp to stringResource(
            R.string.trend_rising,
            trend.consecutiveDays,
            formatSignedPercentagePoints(trend.changeLastMonth),
        )
        is TrendSummary.Falling -> Icons.Filled.TrendingDown to stringResource(
            R.string.trend_falling,
            trend.consecutiveDays,
            formatSignedPercentagePoints(trend.changeLastMonth),
        )
        is TrendSummary.Flat -> Icons.Filled.TrendingFlat to stringResource(R.string.trend_flat, trend.consecutiveDays)
        TrendSummary.New -> Icons.Filled.TrendingFlat to stringResource(R.string.trend_new)
        TrendSummary.InsufficientData -> Icons.Filled.TrendingFlat to stringResource(R.string.trend_insufficient_data)
    }
    Card {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun HistoryRow(point: PositionHistoryPoint) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(formatDate(point.date), style = MaterialTheme.typography.bodyMedium)
        Text(formatPercent(point.totalShortPercent), style = MaterialTheme.typography.bodyMedium)
        Text(
            text = point.changeVsPrevious?.let(::formatSignedPercentagePoints) ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HolderRow(holder: HolderPosition) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(holder.holderName, style = MaterialTheme.typography.bodyMedium)
            Text(
                formatDate(holder.positionDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(formatPercent(holder.percent), style = MaterialTheme.typography.bodyMedium)
        androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
        ChangeIndicator(
            direction = holder.changeDirection,
            changePercentagePoints = holder.previousPercent?.let { holder.percent - it },
        )
    }
}
