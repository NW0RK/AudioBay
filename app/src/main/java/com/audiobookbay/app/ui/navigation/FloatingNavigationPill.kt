package com.audiobookbay.app.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.audiobookbay.app.R
import com.audiobookbay.app.ui.components.EditorialPanel

internal data class BottomNavItem(val label: Int, val icon: ImageVector, val route: NavRoute)

internal val bottomNavItems = listOf(
    BottomNavItem(R.string.nav_home, Icons.Outlined.Home, NavRoute.Home),
    BottomNavItem(R.string.nav_search, Icons.Outlined.Search, NavRoute.Search),
    BottomNavItem(R.string.nav_downloads, Icons.Outlined.FileDownload, NavRoute.Downloads()),
    BottomNavItem(R.string.nav_saved, Icons.Outlined.FavoriteBorder, NavRoute.Saved),
    BottomNavItem(R.string.nav_settings, Icons.Outlined.Settings, NavRoute.Settings),
)

/** Labeled editorial navigation with a single moving rule. */
@Composable
internal fun FloatingNavigationPill(selectedRoute: NavRoute?, onNavigate: (NavRoute) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        val textSize = MaterialTheme.typography.labelSmall.fontSize.value * androidx.compose.ui.platform.LocalDensity.current.fontScale
        val minimumCell = (textSize * 5.8f).dp
        val columns = when { maxWidth / 5 >= minimumCell -> 5; maxWidth / 3 >= minimumCell -> 3; else -> 2 }
        Column(Modifier.fillMaxWidth().selectableGroup()) {
            bottomNavItems.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    row.forEach { item ->
                        val selected = selectedRoute?.let { it::class == item.route::class } == true
                        val tint by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, tween(140), label = "Navigation tint")
                        Column(Modifier.weight(1f).heightIn(min = 68.dp).selectable(selected, role = Role.Tab, onClick = { onNavigate(item.route) }).padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.width(24.dp).height(2.dp).background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent))
                            Spacer(Modifier.height(4.dp))
                            Icon(item.icon, stringResource(item.label), Modifier.size(22.dp), tint = tint)
                            androidx.compose.material3.Text(stringResource(item.label), color = tint, style = MaterialTheme.typography.labelSmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
