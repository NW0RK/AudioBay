package com.audiobookbay.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
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

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: NavRoute,
)

private val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Default.Home, NavRoute.Home),
    BottomNavItem("Search", Icons.Default.Search, NavRoute.Search),
    BottomNavItem("Downloads", Icons.Default.Download, NavRoute.Downloads()),
    BottomNavItem("Settings", Icons.Default.Settings, NavRoute.Settings),
)

@Composable
fun AudioBookBayNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = currentDestination?.hasRoute(item.route::class) == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.Home,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<NavRoute.Home> {
                HomeScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(NavRoute.AudiobookDetail(id))
                    },
                )
            }
            composable<NavRoute.Search> {
                SearchScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(NavRoute.AudiobookDetail(id))
                    },
                )
            }
            composable<NavRoute.Categories> {
                CategoriesScreen(
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
                DownloadsScreen()
            }
            composable<NavRoute.Settings> {
                SettingsScreen()
            }
        }
    }
}
