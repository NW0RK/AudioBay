package com.audiobookbay.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavHostController
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.audiobookbay.app.ui.screens.categories.CategoriesScreen
import com.audiobookbay.app.ui.screens.detail.AudiobookDetailScreen
import com.audiobookbay.app.ui.screens.downloads.DownloadsScreen
import com.audiobookbay.app.ui.screens.home.HomeScreen
import com.audiobookbay.app.ui.screens.search.SearchScreen
import com.audiobookbay.app.ui.screens.settings.SettingsScreen

@Composable
fun AudioBookBayNavHost() {
    val navController = rememberNavController()
    var homeResetToken by rememberSaveable { mutableIntStateOf(0) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.imePadding(),
        bottomBar = {
            if (currentDestination?.hasRoute<NavRoute.AudiobookDetail>() != true) {
                FloatingNavigationPill(
                    selectedRoute = bottomNavItems.firstOrNull {
                        currentDestination?.hasRoute(it.route::class) == true
                    }?.route,
                    onNavigate = { route ->
                        if (route == NavRoute.Home) homeResetToken++
                        navController.navigateToTab(route)
                    },
                    modifier = Modifier.navigationBarsPadding()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 20.dp, top = 8.dp),
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.Home,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            composable<NavRoute.Home> {
                HomeScreen(
                    resetToken = homeResetToken,
                    onCategory = { navController.navigate(NavRoute.Categories(it)) },
                    onDownload = { navController.navigate(NavRoute.Downloads(it)) },
                    onNavigateToDetail = { id ->
                        navController.navigate(NavRoute.AudiobookDetail(id))
                    },
                )
            }
            composable<NavRoute.Search> {
                SearchScreen(
                    onBrowse = { navController.navigateToTab(NavRoute.Home) },
                    onDownload = { navController.navigate(NavRoute.Downloads(it)) },
                    onNavigateToDetail = { id ->
                        navController.navigate(NavRoute.AudiobookDetail(id))
                    },
                )
            }
            composable<NavRoute.Categories> {
                CategoriesScreen(
                    onBack = { navController.popBackStack() },
                    onDownload = { navController.navigate(NavRoute.Downloads(it)) },
                    onNavigateToDetail = { id ->
                        navController.navigate(NavRoute.AudiobookDetail(id))
                    },
                )
            }
            composable<NavRoute.AudiobookDetail> {
                AudiobookDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onDownload = { id -> navController.navigate(NavRoute.Downloads(id)) },
                )
            }
            composable<NavRoute.Downloads> {
                DownloadsScreen(onBrowse = { navController.navigateToTab(NavRoute.Home) })
            }
            composable<NavRoute.Saved> {
                com.audiobookbay.app.ui.screens.saved.SavedScreen(
                    onBrowse = { navController.navigateToTab(NavRoute.Home) },
                    onDownload = { navController.navigate(NavRoute.Downloads(it)) },
                    onNavigateToDetail = { navController.navigate(NavRoute.AudiobookDetail(it)) },
                )
            }
            composable<NavRoute.Settings> {
                SettingsScreen()
            }
        }
    }
}

/** Home is a destination, rather than a request to restore its saved nested stack. */
internal fun NavHostController.navigateToTab(route: NavRoute) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = route != NavRoute.Home
    }
}
