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
import androidx.test.platform.app.InstrumentationRegistry
import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.ui.screens.detail.AudiobookDetailContent
import com.audiobookbay.app.ui.theme.AudioBookBayTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UxRefinementUiTest {
    @get:Rule val compose = createComposeRule()
    private fun detail(dark: Boolean) {
        var prepared = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                AudioBookBayTheme(darkTheme = dark, fontPercent = 150) {
                    Surface(Modifier.fillMaxSize()) {
                        AudiobookDetailContent(UiState.Success(AudiobookDetail("book", "The Bookshop at the Edge of the World", "Emile Laurent",
                            description = "A long description. ".repeat(50), torrentInfo = TorrentInfo(infoHash = "hash", magnetUri = "magnet:?xt=test"))),
                            {}, {}, { prepared++ })
                    }
                }
            }
        }
        compose.onNodeWithText("Prepare download").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, prepared) }
        val directory = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("visual-qa")!!
        directory.mkdirs()
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            File(directory, "detail-${if (dark) "dark" else "light"}-large.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    @Test fun detailActionStaysVisibleAtMaximumTextInDarkMode() { detail(true) }
    @Test fun detailActionStaysVisibleAtMaximumTextInLightMode() { detail(false) }
}
