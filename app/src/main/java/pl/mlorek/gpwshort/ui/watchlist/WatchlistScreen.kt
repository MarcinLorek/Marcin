package pl.mlorek.gpwshort.ui.watchlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.mlorek.gpwshort.R
import pl.mlorek.gpwshort.domain.model.WatchlistItem
import pl.mlorek.gpwshort.ui.components.ChangeIndicator
import pl.mlorek.gpwshort.util.formatPercent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    onIssuerClick: (Long) -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val items by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.watchlist_title)) }) }) { padding ->
        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.watchlist_empty),
                modifier = Modifier.padding(padding).fillMaxSize().padding(32.dp),
                textAlign = TextAlign.Center,
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items, key = { it.issuer.id }) { item ->
                    WatchlistRow(item = item, onClick = { onIssuerClick(item.issuer.id) })
                }
            }
        }
    }
}

@Composable
private fun WatchlistRow(item: WatchlistItem, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.issuer.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.watchlist_threshold_label, "%.2f".format(item.alertThresholdPercentagePoints)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val summary = item.latestSummary
            if (summary != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatPercent(summary.totalShortPercent), style = MaterialTheme.typography.titleMedium)
                    ChangeIndicator(
                        direction = summary.changeDirection,
                        changePercentagePoints = summary.changePercentagePoints,
                    )
                }
            }
        }
    }
}
