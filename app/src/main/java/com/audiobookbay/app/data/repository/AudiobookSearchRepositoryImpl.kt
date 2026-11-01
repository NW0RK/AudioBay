package com.audiobookbay.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
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

    private val searchHistoryKey = stringSetPreferencesKey("search_history_queries")

    override suspend fun searchAudiobooks(query: String, page: Int): Resource<List<Audiobook>> {
        if (query.isNotBlank()) {
            saveSearchQuery(query)
        }
        return remoteDataSource.searchAudiobooks(query, page)
    }

    override fun getSearchHistory(): Flow<List<String>> {
        return dataStore.data.map { preferences ->
            preferences[searchHistoryKey]?.toList() ?: emptyList()
        }
    }

    override suspend fun saveSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        dataStore.edit { preferences ->
            val current = preferences[searchHistoryKey]?.toMutableSet() ?: mutableSetOf()
            current.add(trimmed)
            preferences[searchHistoryKey] = current
        }
    }

    override suspend fun clearSearchHistory() {
        dataStore.edit { preferences ->
            preferences.remove(searchHistoryKey)
        }
    }
}
