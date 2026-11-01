package com.audiobookbay.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.audiobookbay.app.data.storage.ApiKeyFailure
import com.audiobookbay.app.domain.repository.*
import com.audiobookbay.app.ui.screens.settings.ApiKeySettingsCard
import com.audiobookbay.app.ui.screens.settings.AppUpdateCard
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsUpdateUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun failedKeySaveRetainsInputAndShowsStorageError() {
        var input by mutableStateOf("")
        var failure by mutableStateOf<ApiKeyFailure?>(null)
        compose.setContent { MaterialTheme {
            ApiKeySettingsCard(input, true, false, false, failure, { input = it },
                { failure = ApiKeyFailure.STORAGE }, {})
        } }
        compose.onNodeWithTag("gemini_api_key").performTextInput("synthetic-key")
        compose.onNodeWithText("Save key").performClick()
        compose.onNodeWithText("Could not store the key on this device.", substring = true).assertExists()
        compose.runOnIdle { assertEquals("synthetic-key", input) }
    }

    @Test fun updateDownloadRequiresTapAndCanBeCancelled() {
        val update = AppUpdate("0.1.2", "", "app.apk", "", "", 100)
        var state by mutableStateOf(AppUpdateState(UpdatePhase.AVAILABLE, update))
        var downloads = 0
        compose.setContent { MaterialTheme {
            AppUpdateCard(state, false, false, {}, { downloads++; state = state.copy(phase = UpdatePhase.DOWNLOADING) },
                { state = state.copy(phase = UpdatePhase.AVAILABLE) }, {})
        } }
        compose.runOnIdle { assertEquals(0, downloads) }
        compose.onNodeWithText("Download update").performClick()
        compose.onNodeWithText("Cancel download").performClick()
        compose.onNodeWithText("Download update").assertExists()
        compose.runOnIdle { assertEquals(1, downloads) }
    }

    @Test fun installationPermissionFailureKeepsInstallActionAvailable() {
        compose.setContent { MaterialTheme {
            AppUpdateCard(AppUpdateState(UpdatePhase.READY), false, true, {}, {}, {}, {})
        } }
        compose.onNodeWithText("Installation did not start.", substring = true).assertExists()
        compose.onNodeWithText("Install update").assertIsEnabled()
    }
}
