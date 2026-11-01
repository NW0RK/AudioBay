package com.audiobookbay.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.audiobookbay.app.ui.screens.downloads.*
import com.audiobookbay.app.ui.theme.AudioBookBayTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DownloadSetupFormTest {
    @get:Rule val compose = createComposeRule()
    private var state by mutableStateOf(DownloadSetupState(
        title = "A book", author = "An author", magnet = "magnet:?xt=urn:btih:test",
        folderLabel = "Internal storage/Android/AudioBooks", folderSelected = true,
        catalogBook = true, expanded = true,
    ))
    private var started = 0
    private var picked = 0
    private var imported = 0
    private var resumed = 0
    private var corrected = 0
    private var undone = 0

    private fun render(largeText: Boolean = false) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeText) 1.8f else density.fontScale)) {
                AudioBookBayTheme {
                    Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                        DownloadSetupForm(state,
                            onToggleExpanded = { state = state.copy(expanded = !state.expanded) },
                            onToggleMetadata = { state = state.copy(editingMetadata = !state.editingMetadata) },
                            onToggleLink = { state = state.copy(editingLink = !state.editingLink) },
                            onToggleFolder = { state = state.copy(showingFolder = !state.showingFolder) },
                            onAuthorChange = { state = state.copy(author = it) },
                            onTitleChange = { state = state.copy(title = it) },
                            onMagnetChange = { state = state.copy(magnet = it) },
                            onChooseFolder = { picked++ }, onStart = { started++ },
                            onOpenTorrent = { imported++ }, onSaveResume = { resumed++ },
                            onCancelEdit = {}, onDismissError = { state = state.copy(error = null) },
                            onFixNames = { corrected++ }, onUndoNames = { undone++ })
                    }
                }
            }
        }
    }

    @Test fun catalogSummaryHidesFieldsAndAllowsEditing() {
        render()
        compose.onNodeWithText("A book").assertExists()
        compose.onNodeWithTag("download_title").assertDoesNotExist()
        compose.onNodeWithTag("download_magnet").assertDoesNotExist()
        compose.onNodeWithTag("edit_download_book").performClick()
        compose.onNodeWithTag("download_title").performTextReplacement("Revised title")
        compose.onNodeWithText("Done").performScrollTo().performClick()
        compose.onNodeWithText("Revised title").assertExists()
        compose.onNodeWithTag("edit_download_link").performScrollTo().performClick()
        compose.onNodeWithTag("download_magnet").assertExists()
    }

    @Test fun correctionProgressBlocksSubmissionButAllowsNameEditing() {
        state = state.copy(correctionAvailable = true, correcting = true, editingMetadata = true)
        render(largeText = true)
        compose.onNodeWithTag("correcting_book_names").performScrollTo().assertExists()
        compose.onNodeWithTag("fix_book_names").assertIsNotEnabled()
        compose.onNodeWithTag("start_download").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("download_title").performScrollTo().performTextReplacement("My title")
        compose.runOnIdle { assertEquals("My title", state.title) }
    }

    @Test fun fixAndUndoAreAccessibleAndFailureKeepsDownloadAvailable() {
        state = state.copy(correctionAvailable = true, canUndoNames = true, correctionFailed = true)
        render()
        compose.onNodeWithTag("fix_book_names").performScrollTo().performClick()
        compose.onNodeWithTag("undo_book_names").performScrollTo().performClick()
        compose.onNodeWithTag("start_download").performScrollTo().assertIsEnabled()
        compose.runOnIdle { assertEquals(1, corrected); assertEquals(1, undone) }
    }

    @Test fun missingMetadataAndFolderBlockStarting() {
        state = state.copy(author = "", folderSelected = false)
        render()
        compose.onNodeWithTag("download_author").assertExists()
        compose.onNodeWithTag("start_download").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Choose", substring = false).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, picked); state = state.copy(author = "An author", folderSelected = true) }
        compose.onNodeWithTag("start_download").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, started) }
    }

    @Test fun folderDetailsAreExpandableAndBusyPreventsActions() {
        render()
        compose.onNodeWithTag("download_full_location").assertDoesNotExist()
        compose.onNodeWithText("Show full location").performScrollTo().performClick()
        compose.onNodeWithTag("download_full_location").assertTextEquals("Internal storage/Android/AudioBooks")
        compose.runOnIdle { state = state.copy(busy = true) }
        compose.onNodeWithTag("start_download").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Change").performScrollTo().assertIsNotEnabled()
    }

    @Test fun manualImportKeepsFieldsAndTorrentOption() {
        state = state.copy(catalogBook = false, magnet = "")
        render()
        compose.onNodeWithTag("download_title").assertExists()
        compose.onNodeWithTag("download_magnet").assertExists()
        compose.onNodeWithTag("start_download").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Open torrent").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, imported) }
    }

    @Test fun editingDownloadOffersSaveAndErrorsStayInline() {
        state = state.copy(editingDownload = true, expanded = false, error = DownloadFormError.FOLDER)
        render()
        compose.onNodeWithTag("download_title").assertExists()
        compose.onNodeWithTag("download_magnet").assertDoesNotExist()
        compose.onNodeWithText("Dismiss").performScrollTo().performClick()
        compose.onNodeWithText("Save and resume").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(null, state.error); assertEquals(1, resumed) }
    }

    @Test fun largeTextOnNarrowScreenKeepsActionReachable() {
        state = state.copy(title = "A long book title that wraps across several lines", author = "A long author name")
        render(largeText = true)
        compose.onNodeWithTag("start_download").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, started) }
    }
}
