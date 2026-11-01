package com.audiobookbay.app

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.*
import com.audiobookbay.app.ui.navigation.NavRoute
import com.audiobookbay.app.ui.navigation.navigateToTab
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class HomeNavigationTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var nav: NavHostController
    private fun render() {
        compose.setContent {
            nav = rememberNavController()
            NavHost(nav, startDestination = NavRoute.Home) {
                composable<NavRoute.Home> { Text("Discovery") }
                composable<NavRoute.AudiobookDetail> { Text("Book") }
                composable<NavRoute.Downloads> { Text("Downloads") }
                composable<NavRoute.Search> { Text("Search") }
                composable<NavRoute.Settings> { Text("Settings") }
                composable<NavRoute.Saved> { Text("Saved") }
            }
        }
    }
    @Test fun homeDoesNotRestorePreviouslySavedBookStack() {
        render()
        compose.runOnIdle { nav.navigate(NavRoute.AudiobookDetail("book")) }
        compose.runOnIdle { nav.navigateToTab(NavRoute.Search) }
        compose.runOnIdle { nav.navigateToTab(NavRoute.Home) }
        compose.runOnIdle { assertTrue(nav.currentDestination!!.hasRoute<NavRoute.Home>()) }
    }
    @Test fun homeAlwaysReturnsFromDownloadFlowAndOtherTabsWithoutDuplicates() {
        render()
        compose.runOnIdle { nav.navigate(NavRoute.AudiobookDetail("book")) }
        compose.runOnIdle { nav.navigate(NavRoute.Downloads("book")) }
        compose.runOnIdle { nav.navigateToTab(NavRoute.Home) }
        compose.runOnIdle { assertTrue(nav.currentDestination!!.hasRoute<NavRoute.Home>()) }
        for (route in listOf(NavRoute.Settings, NavRoute.Saved, NavRoute.Search, NavRoute.Home)) {
            compose.runOnIdle { nav.navigateToTab(route) }
            compose.runOnIdle { nav.navigateToTab(NavRoute.Home) }
            compose.runOnIdle { assertTrue(nav.currentDestination!!.hasRoute<NavRoute.Home>()); assertNull(nav.previousBackStackEntry) }
        }
    }
    @Test fun searchDoesNotRestorePreviouslySavedDetailStack() {
        render()
        compose.runOnIdle { nav.navigateToTab(NavRoute.Search) }
        compose.runOnIdle { nav.navigate(NavRoute.AudiobookDetail("book")) }
        compose.runOnIdle { nav.navigateToTab(NavRoute.Home) }
        compose.runOnIdle { nav.navigateToTab(NavRoute.Search) }
        compose.runOnIdle { assertTrue(nav.currentDestination!!.hasRoute<NavRoute.Search>()) }
        compose.runOnIdle { nav.navigateToTab(NavRoute.Search) }
        compose.runOnIdle { assertTrue(nav.currentDestination!!.hasRoute<NavRoute.Search>()) }
    }
}
