package com.audiobookbay.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.audiobookbay.app.ui.screens.settings.SearchFormattingSettingsCard
import com.audiobookbay.app.ui.theme.AudioBookBayTheme
import org.junit.Rule
import org.junit.Test

class SearchFormattingUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun fullRowToggleAndSaveErrorRemainAccessibleAtLargeText() {
        compose.setContent {
            var enabled by remember { mutableStateOf(false) }
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                AudioBookBayTheme(darkTheme = false, fontPercent = 150) {
                    Surface {
                        Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                            SearchFormattingSettingsCard(enabled, true) { enabled = it }
                        }
                    }
                }
            }
        }
        compose.onNode(isToggleable()).performScrollTo().assertIsOff().performClick().assertIsOn()
        compose.onNodeWithText("Could not save this setting. Try again.").performScrollTo().assertIsDisplayed()
    }
}
