package pl.mlorek.gpwshort.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.PullRefreshIndicator
import androidx.compose.material.pullRefresh
import androidx.compose.material.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Wrapper na `androidx.compose.material.pullrefresh` (Material 2), zamiast
 * `androidx.compose.material3.PullToRefreshBox`.
 *
 * Ta ostatnia funkcja okazała się nierozpoznawalna ("Unresolved reference") przy
 * kompilacji z Compose BOM użytym w tym projekcie - zamiast dalej zgadywać dokładną
 * wersję BOM/material3, w której ten konkretny composable się pojawił, korzystamy z
 * odpowiednika, który jest częścią Compose od bardzo dawna i ma identyczną nazwę oraz
 * sygnaturę wywołania, więc ekrany korzystające z tej funkcji nie wymagały żadnych
 * innych zmian poza podmianą importu.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = onRefresh)
    Box(modifier = modifier.fillMaxSize().pullRefresh(state)) {
        content()
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = state,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
