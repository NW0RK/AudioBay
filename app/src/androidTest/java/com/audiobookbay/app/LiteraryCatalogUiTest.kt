package com.audiobookbay.app

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import com.audiobookbay.app.ui.screens.home.HomeScreen
import com.audiobookbay.app.ui.screens.home.HomeContent
import com.audiobookbay.app.ui.screens.detail.AudiobookDetailContent
import org.junit.Assert.assertTrue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.unit.dp
import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.ui.components.*
import com.audiobookbay.app.ui.screens.saved.SavedContent
import com.audiobookbay.app.ui.theme.AudioBookBayTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LiteraryCatalogUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun emptySavedOffersWorkingBrowseAction() {
        var browsed = 0
        compose.setContent { AudioBookBayTheme { SavedContent(UiState.Success(emptyList()), {}, { browsed++ }) } }
        compose.onNodeWithText("Browse audiobooks").performClick()
        compose.runOnIdle { assertEquals(1, browsed) }
    }
    @Test fun cardActionsDoNotTriggerDetailNavigation() {
        var details = 0; var downloads = 0; var saves = 0
        compose.setContent { AudioBookBayTheme {
            AudiobookCard(Audiobook("id", "Example", "Author"), Modifier.width(180.dp), state = BookCardState(saveEnabled = true),
                onSave = { saves++ }, onDownload = { downloads++ }, onClick = { details++ })
        } }
        compose.onNodeWithContentDescription("Prepare download for Example").performClick()
        compose.onNodeWithContentDescription("Save Example for later").performClick()
        compose.runOnIdle { assertEquals(0, details); assertEquals(1, downloads); assertEquals(1, saves) }
        compose.onNodeWithText("Example").performClick()
        compose.runOnIdle { assertEquals(1, details) }
    }
    @Test fun savedLayoutAndSortSurviveRecreation() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { AudioBookBayTheme {
            SavedContent(UiState.Success(listOf(Audiobook("1", "Long title — წიგნი", "Author"))), {}, {})
        } }
        compose.onNodeWithContentDescription("List view").performClick().assertIsOn()
        compose.onNodeWithText("Recently saved").performClick()
        compose.onNodeWithText("Title").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("List view").assertIsOn()
        compose.onNodeWithText("Title").assertIsDisplayed()
    }
    @Test fun errorStateRetries() {
        var retries = 0
        compose.setContent { AudioBookBayTheme(darkTheme = false) {
            MessageState("Unavailable", "Try again", "Retry", { retries++ }, error = true)
        } }
        compose.onNodeWithText("Retry").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun homeReselectionResetsCatalogScroll() {
        val reset = mutableIntStateOf(0)
        val books = (0..39).map { Audiobook(it.toString(), "Book " + it, "Author") }
        compose.setContent { AudioBookBayTheme {
            HomeScreen(UiState.Success(HomeContent(books = books, endReached = true)), {}, {}, {}, {}, reset.intValue)
        } }
        compose.onNodeWithTag("discovery_grid").performScrollToNode(hasText("Book 39"))
        compose.onNodeWithText("Book 39").assertIsDisplayed()
        compose.runOnIdle { reset.intValue++ }
        compose.onNodeWithText(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.recently_added)).assertIsDisplayed()
    }
    @Test fun coverCollapsesAndBackRemainsAvailable() {
        var back = 0
        compose.setContent { AudioBookBayTheme {
            AudiobookDetailContent(UiState.Success(AudiobookDetail("book", "A long literary title", "Author",
                description = "A chapter of an extraordinary journey. ".repeat(100))), {}, { back++ }, {})
        } }
        compose.onNodeWithTag("detail_content").performTouchInput { swipeUp() }
        val covers = compose.onAllNodesWithTag("detail_cover").fetchSemanticsNodes()
        assertTrue(covers.isEmpty() || covers.first().boundsInRoot.top < 0f)
        compose.onNodeWithContentDescription("Back").performClick()
        compose.runOnIdle { assertEquals(1, back) }
    }
    @Test fun homeMetadataPlacesSizeBelowLanguageAndFormat() {
        compose.setContent { AudioBookBayTheme {
            AudiobookCard(Audiobook("id", "Example", "Author", language = "English", audioFormat = "M4B", formattedSize = "150 MB"),
                Modifier.width(156.dp), homeMetadata = true, onClick = {})
        } }
        val language = compose.onNodeWithText("English", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val format = compose.onNodeWithText("M4B", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val size = compose.onNodeWithText("150 MB", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(language.top, format.top)
        assertTrue(size.top >= language.bottom)
    }
}
