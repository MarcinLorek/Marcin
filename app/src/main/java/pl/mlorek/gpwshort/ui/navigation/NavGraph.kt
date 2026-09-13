package pl.mlorek.gpwshort.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pl.mlorek.gpwshort.R
import pl.mlorek.gpwshort.ui.detail.IssuerDetailScreen
import pl.mlorek.gpwshort.ui.list.IssuerListScreen
import pl.mlorek.gpwshort.ui.watchlist.WatchlistScreen

@Composable
fun GpwShortNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == Destinations.LIST || currentRoute == Destinations.WATCHLIST

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Destinations.LIST,
                        onClick = {
                            navController.navigate(Destinations.LIST) {
                                popUpTo(Destinations.LIST) { inclusive = true }
                            }
                        },
                        icon = { Icon(Icons.Filled.List, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_list)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Destinations.WATCHLIST,
                        onClick = {
                            navController.navigate(Destinations.WATCHLIST) { launchSingleTop = true }
                        },
                        icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_watchlist)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.LIST,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destinations.LIST) {
                IssuerListScreen(onIssuerClick = { issuerId ->
                    navController.navigate(Destinations.detailRoute(issuerId))
                })
            }
            composable(Destinations.WATCHLIST) {
                WatchlistScreen(onIssuerClick = { issuerId ->
                    navController.navigate(Destinations.detailRoute(issuerId))
                })
            }
            composable(
                route = Destinations.DETAIL,
                arguments = listOf(navArgument(Destinations.DETAIL_ARG_ISSUER_ID) { type = NavType.LongType }),
            ) {
                IssuerDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
