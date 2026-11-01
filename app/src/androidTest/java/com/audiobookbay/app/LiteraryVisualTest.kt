package com.audiobookbay.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.ui.components.*
import com.audiobookbay.app.ui.navigation.FloatingNavigationPill
import com.audiobookbay.app.ui.navigation.NavRoute
import com.audiobookbay.app.ui.screens.saved.SavedContent
import com.audiobookbay.app.ui.theme.AudioBookBayTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Deterministic, offline fixtures for visual review on the validation device. */
class LiteraryVisualTest {
    @get:Rule val compose = createComposeRule()
    private fun capture(name: String) {
        val directory = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("visual-qa")!!
        directory.mkdirs()
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            File(directory, name + ".png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    private fun render(dark: Boolean, scale: Float, empty: Boolean) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                AudioBookBayTheme(darkTheme = dark) {
                    Surface {
                        Column(Modifier.fillMaxSize().padding(16.dp)) {
                            Box(Modifier.weight(1f)) {
                                if (empty) SavedContent(UiState.Success(emptyList()), {}, {})
                                else AudiobookCard(Audiobook("1", "The Bookshop at the Edge of the World — წიგნი", "Émile Laurent",
                                    language = "English", audioFormat = "M4B"),
                                    compact = true, state = BookCardState(saved = true, saveEnabled = true,
                                        download = DownloadItem("1", "1", "Book", "hash", "", DownloadStatus.PUBLISHING, progress = 1f, publicationProgress = 0.45f)), onClick = {})
                            }
                            FloatingNavigationPill(NavRoute.Saved, {})
                        }
                    }
                }
            }
        }
    }
    @Test fun darkCardAndPublicationProgress() { render(true, 1f, false); capture("dark-card") }
    @Test fun lightCardAndUnicodeAtLargeType() { render(false, 2f, false); capture("light-card-large-type") }
    @Test fun darkEmptyStateAtLargeType() {
        render(true, 2f, true)
        compose.onNodeWithText("Browse audiobooks").performScrollTo().assertIsDisplayed()
        capture("dark-empty-large-type")
    }
    @Test fun lightEmptyState() { render(false, 1f, true); capture("light-empty") }
}
