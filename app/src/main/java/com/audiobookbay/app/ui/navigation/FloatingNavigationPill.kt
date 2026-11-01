package com.audiobookbay.app.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.audiobookbay.app.R

internal data class BottomNavItem(val label: Int, val icon: ImageVector, val route: NavRoute)

internal val bottomNavItems = listOf(
    BottomNavItem(R.string.nav_home, Icons.Outlined.Home, NavRoute.Home),
    BottomNavItem(R.string.nav_search, Icons.Outlined.Search, NavRoute.Search),
    BottomNavItem(R.string.nav_downloads, Icons.Outlined.FileDownload, NavRoute.Downloads()),
    BottomNavItem(R.string.nav_saved, Icons.Outlined.FavoriteBorder, NavRoute.Saved),
    BottomNavItem(R.string.nav_settings, Icons.Outlined.Settings, NavRoute.Settings),
)

/** Five icon-only destinations in one row, with labels retained for accessibility. */
@Composable
internal fun FloatingNavigationPill(selectedRoute: NavRoute?, onNavigate: (NavRoute) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).selectableGroup()) {
        bottomNavItems.forEach { item ->
            val selected = selectedRoute?.let { it::class == item.route::class } == true
            val tint by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                tween(140), label = "Navigation tint",
            )
            Box(
                Modifier.weight(1f).height(56.dp)
                    .selectable(selected, role = Role.Tab, onClick = { onNavigate(item.route) }),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Box(Modifier.align(Alignment.TopCenter).width(24.dp).height(2.dp).background(MaterialTheme.colorScheme.primary))
                Icon(item.icon, stringResource(item.label), Modifier.size(24.dp), tint = tint)
            }
        }
    }
}
