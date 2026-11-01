package com.audiobookbay.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.audiobookbay.app.data.storage.DownloadNetworkSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DownloadNetworkSettingsTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var settings: DownloadNetworkSettings

    @Before fun setUp() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(temporaryFolder.root, "network.preferences_pb")
        }
        settings = DownloadNetworkSettings(dataStore, Dispatchers.IO)
    }

    @After fun tearDown() { scope.cancel() }

    @Test fun wifiOnlyDefaultsOff() = runBlocking {
        assertFalse(settings.wifiOnly.first())
    }

    @Test fun wifiOnlyIsPersistedAcrossObservers() = runBlocking {
        settings.setWifiOnly(true)
        assertTrue(settings.wifiOnly.first())
        settings.setWifiOnly(false)
        assertFalse(settings.wifiOnly.first())
    }
}
