package com.audiobookbay.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.audiobookbay.app.ui.navigation.FloatingNavigationPill
import com.audiobookbay.app.ui.navigation.NavRoute
import com.audiobookbay.app.ui.navigation.bottomNavItems
import com.audiobookbay.app.ui.theme.AudioBookBayTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FloatingNavigationPillTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allTabsAreAccessibleAndDispatchSelection() {
        val selected = mutableStateOf<NavRoute>(NavRoute.Home)
        var labels = emptyList<String>()
        compose.setContent {
            labels = bottomNavItems.map { stringResource(it.label) }
            AudioBookBayTheme {
                FloatingNavigationPill(selected.value, { selected.value = it })
            }
        }
        compose.onAllNodes(isSelectable()).assertCountEquals(5)
        bottomNavItems.forEachIndexed { index, item ->
            val tab = compose.onAllNodes(isSelectable())[index]
            tab.assertHasClickAction().assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            compose.onNodeWithContentDescription(labels[index]).assertExists()
            tab.performClick().assertIsSelected()
            compose.runOnIdle { assertEquals(item.route, selected.value) }
            compose.onAllNodes(isSelected()).assertCountEquals(1)
        }
    }

    private fun verifyLayout(width: Int, dark: Boolean) {
        compose.setContent {
            AudioBookBayTheme(darkTheme = dark) {
                Box(Modifier.width(width.dp)) {
                    FloatingNavigationPill(NavRoute.Home, {})
                }
            }
        }
        val bounds = compose.onAllNodes(isSelectable()).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(5, bounds.size)
        bounds.forEach { assertEquals(bounds.first().width, it.width, 1f) }
        bounds.groupBy { it.top }.values.forEach { row ->
            row.zipWithNext().forEach { (left, right) -> assertEquals(left.right, right.left, 1f) }
        }
    }

    @Test fun narrowDarkLayoutHasEqualSpacing() = verifyLayout(288, true)
    @Test fun wideLightLayoutHasEqualSpacing() = verifyLayout(800, false)
}
