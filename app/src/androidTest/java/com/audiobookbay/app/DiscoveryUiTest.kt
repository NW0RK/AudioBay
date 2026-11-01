package com.audiobookbay.app
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.audiobookbay.app.ui.screens.home.DiscoverySection
import com.audiobookbay.app.ui.theme.AudioBookBayTheme
import com.audiobookbay.app.domain.model.*
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
class DiscoveryUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun noHistoryPreventsGeneration() {
        var calls = 0
        compose.setContent { AudioBookBayTheme { DiscoverySection(emptyList(), false, true, false, null, {calls++}, {}, {}) } }
        compose.onNodeWithText("Generate picks").assertIsNotEnabled()
        assertEquals(0, calls)
    }
    @Test fun missingKeyOpensSettings() {
        var settings = false
        compose.setContent { AudioBookBayTheme { DiscoverySection(emptyList(), true, false, false, null, {}, {settings=true}, {}) } }
        compose.onNodeWithText("Set up AI").performClick()
        compose.runOnIdle { assertTrue(settings) }
    }
    @Test fun busyKeepsPreviousRecommendationAndDisablesGeneration() {
        val pick = DiscoveryPick(Audiobook("real", "Verified book", "Author"), "A new direction")
        compose.setContent { AudioBookBayTheme { DiscoverySection(listOf(pick), true, true, true, null, {}, {}, {}) } }
        compose.onNodeWithText("Verified book").assertIsDisplayed()
        compose.onNodeWithText("Finding your next books…").assertIsNotEnabled()
    }
}
