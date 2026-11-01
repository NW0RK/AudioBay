package com.audiobookbay.app.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.audiobookbay.app.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persisted network constraints applied to torrent transfers.
 *
 * Wi-Fi-only is deliberately the only constraint the product exposes: transfers wait for an
 * unmetered connection instead of consuming mobile data. The torrent engine owns enforcement.
 */
@Singleton
class DownloadNetworkSettings @Inject constructor(
    private val preferences: DataStore<Preferences>,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    val wifiOnly: Flow<Boolean> = preferences.data
        .map { it[WIFI_ONLY] ?: false }
        .distinctUntilChanged()

    suspend fun setWifiOnly(enabled: Boolean) = withContext(io) {
        preferences.edit { it[WIFI_ONLY] = enabled }
    }

    companion object {
        internal val WIFI_ONLY = booleanPreferencesKey("wifi_only_downloads")
    }
}
