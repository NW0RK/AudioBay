package com.audiobookbay.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.audiobookbay.app.data.remote.AudiobookRemoteDataSource
import com.audiobookbay.app.domain.model.Audiobook
import com.audiobookbay.app.domain.model.Resource
import com.audiobookbay.app.domain.repository.AudiobookSearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudiobookSearchRepositoryImpl @Inject constructor(
    private val remoteDataSource: AudiobookRemoteDataSource,
    private val dataStore: DataStore<Preferences>,
) : AudiobookSearchRepository {

    private val legacySearchHistoryKey = stringSetPreferencesKey("search_history_queries")
    private val searchHistoryKey = stringPreferencesKey("search_history_ordered")
    private fun readHistory(preferences: Preferences): List<String> =
        preferences[searchHistoryKey]?.let { runCatching { Json.decodeFromString<List<String>>(it) }.getOrNull() }
            ?: preferences[legacySearchHistoryKey]?.toList().orEmpty()

    override suspend fun searchAudiobooks(query: String, page: Int): Resource<List<Audiobook>> {
        if (query.isNotBlank()) {
            saveSearchQuery(query)
        }
        return remoteDataSource.searchAudiobooks(query, page)
    }

    override fun getSearchHistory(): Flow<List<String>> {
        return dataStore.data.map { preferences ->
            readHistory(preferences)
        }
    }

    override suspend fun saveSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        dataStore.edit { preferences ->
            val current = readHistory(preferences).filterNot { it.equals(trimmed, ignoreCase = true) }
            preferences[searchHistoryKey] = Json.encodeToString((listOf(trimmed) + current).take(20))
            preferences.remove(legacySearchHistoryKey)
        }
    }

    override suspend fun clearSearchHistory() {
        dataStore.edit { preferences ->
            preferences.remove(searchHistoryKey)
            preferences.remove(legacySearchHistoryKey)
        }
    }
}
