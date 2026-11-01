package com.audiobookbay.app

import androidx.datastore.preferences.core.*
import com.audiobookbay.app.data.storage.AppearanceSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AppearanceSettingsTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun defaultsPersistenceClampingAndReset() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        try {
            val store = PreferenceDataStoreFactory.create(scope = scope) { File(temporaryFolder.root, "appearance.preferences_pb") }
            val settings = AppearanceSettings(store, Dispatchers.IO)
            val search = com.audiobookbay.app.data.storage.SearchFormattingSettings(store, Dispatchers.IO)
            assertFalse(search.enabled.first())
            search.setEnabled(true)
            assertTrue(com.audiobookbay.app.data.storage.SearchFormattingSettings(store, Dispatchers.IO).enabled.first())
            search.setEnabled(false)
            assertFalse(search.enabled.first())
            assertEquals(100, settings.fontPercent.first())
            assertEquals("dark", settings.theme.first())
            settings.setTheme("system")
            assertEquals("system", AppearanceSettings(store, Dispatchers.IO).theme.first())
            store.edit { it[AppearanceSettings.THEME] = "unknown" }
            assertEquals("dark", settings.theme.first())
            settings.setFontPercent(123)
            assertEquals(123, AppearanceSettings(store, Dispatchers.IO).fontPercent.first())
            settings.setFontPercent(300)
            assertEquals(150, settings.fontPercent.first())
            settings.setFontPercent(1)
            assertEquals(80, settings.fontPercent.first())
            store.edit { it[AppearanceSettings.FONT_PERCENT] = 999 }
            assertEquals(150, settings.fontPercent.first())
            settings.setFontPercent(100)
            assertEquals(100, settings.fontPercent.first())
        } finally { scope.cancel() }
    }
}
