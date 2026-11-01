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
import com.audiobookbay.app.ui.components.GlassPanel

internal data class BottomNavItem(val label: Int, val icon: ImageVector, val route: NavRoute)

internal val bottomNavItems = listOf(
    BottomNavItem(R.string.nav_home, Icons.Outlined.Home, NavRoute.Home),
    BottomNavItem(R.string.nav_search, Icons.Outlined.Search, NavRoute.Search),
    BottomNavItem(R.string.nav_downloads, Icons.Outlined.FileDownload, NavRoute.Downloads()),
    BottomNavItem(R.string.nav_saved, Icons.Outlined.FavoriteBorder, NavRoute.Saved),
    BottomNavItem(R.string.nav_settings, Icons.Outlined.Settings, NavRoute.Settings),
)

/** Fixed-size highlights keep the glass pill calm while selection changes. */
@Composable
internal fun FloatingNavigationPill(
    selectedRoute: NavRoute?,
    onNavigate: (NavRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        GlassPanel(
            modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth(),
            shape = CircleShape,
        ) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp).selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                bottomNavItems.forEach { item ->
                    val selected = selectedRoute?.let { it::class == item.route::class } == true
                    val tint by animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        tween(180), label = "Navigation icon tint",
                    )
                    val alpha by animateFloatAsState(
                        if (selected) 0.18f else 0f, tween(180), label = "Navigation highlight",
                    )
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clip(CircleShape)
                            .selectable(selected, role = Role.Tab, onClick = { onNavigate(item.route) }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha), CircleShape))
                        Icon(item.icon, stringResource(item.label), Modifier.size(24.dp), tint = tint)
                    }
                }
            }
        }
    }
}
