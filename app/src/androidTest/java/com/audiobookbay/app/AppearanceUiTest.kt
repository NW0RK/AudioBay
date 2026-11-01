package com.audiobookbay.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.audiobookbay.app.domain.model.Audiobook
import com.audiobookbay.app.ui.components.AudiobookCard
import com.audiobookbay.app.ui.screens.settings.AppearanceSettingsCard
import com.audiobookbay.app.ui.theme.AudioBookBayTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class AppearanceUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun metadataSeparatorOnlyAppearsBetweenPresentFields() {
        val language = mutableStateOf<String?>("English")
        val format = mutableStateOf<String?>("M4B")
        compose.setContent { AudioBookBayTheme {
            AudiobookCard(Audiobook("id", "Book", "Author", language = language.value, audioFormat = format.value),
                compact = true, homeMetadata = true, onClick = {})
        } }
        compose.onNodeWithText(" \u00B7 ").assertIsDisplayed()
        compose.onNodeWithText("English").assertIsDisplayed()
        compose.onNodeWithText("M4B").assertIsDisplayed()
        compose.runOnIdle { language.value = null }
        compose.onNodeWithText(" \u00B7 ").assertDoesNotExist()
        compose.onNodeWithText("M4B").assertIsDisplayed()
        compose.runOnIdle { language.value = "English"; format.value = null }
        compose.onNodeWithText(" \u00B7 ").assertDoesNotExist()
        compose.onNodeWithText("English").assertIsDisplayed()
        compose.runOnIdle { language.value = null }
        compose.onNodeWithText(" \u00B7 ").assertDoesNotExist()
    }

    @Test fun sliderResetAndErrorRemainAccessibleWithLargeSystemText() {
        val percent = mutableIntStateOf(100)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                AudioBookBayTheme(fontPercent = percent.intValue) {
                    Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                        AppearanceSettingsCard(percent.intValue, true) { percent.intValue = it }
                    }
                }
            }
        }
        for (size in listOf(80, 100, 150)) {
            compose.runOnIdle { percent.intValue = size }
            compose.onNodeWithContentDescription("Font size").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Font size").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(123f) }
            compose.runOnIdle { assertEquals(123, percent.intValue) }
            compose.onNodeWithText("Reset to 100%").performScrollTo().performClick()
            compose.runOnIdle { assertEquals(100, percent.intValue) }
            compose.onNodeWithText("Could not save font size. Please try again.").performScrollTo().assertIsDisplayed()
        }
    }
    @Test fun dialogActionsRemainAvailableAtFontLimits() {
        val percent = mutableIntStateOf(80)
        compose.setContent { AudioBookBayTheme(fontPercent = percent.intValue) {
            AlertDialog(onDismissRequest = {}, title = { Text("Download audiobook") },
                text = { Text("Choose the audiobook tracks to download.") },
                confirmButton = { TextButton(onClick = {}) { Text("Download") } },
                dismissButton = { TextButton(onClick = {}) { Text("Cancel") } })
        } }
        for (size in listOf(80, 100, 150)) {
            compose.runOnIdle { percent.intValue = size }
            compose.onNodeWithText("Download").assertIsDisplayed().performClick()
            compose.onNodeWithText("Cancel").assertIsDisplayed().performClick()
        }
    }

}
