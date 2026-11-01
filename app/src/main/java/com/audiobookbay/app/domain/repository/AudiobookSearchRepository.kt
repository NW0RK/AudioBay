package com.audiobookbay.app.domain.repository

import com.audiobookbay.app.domain.model.Audiobook
import com.audiobookbay.app.domain.model.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Contract for searching audiobooks with debouncing, query caching, and search history.
 */
interface AudiobookSearchRepository {

    /**
     * Executes a keyword search against remote sources.
     *
     * @param query Search keywords (title, author, narrator)
     * @param page Page index (1-based)
     */
    suspend fun searchAudiobooks(query: String, page: Int = 1): Resource<List<Audiobook>>

    /**
     * Observes recent search query history.
     */
    fun getSearchHistory(): Flow<List<String>>

    /**
     * Saves a query into local search history.
     */
    suspend fun saveSearchQuery(query: String)

    /**
     * Clears all recorded search history.
     */
    suspend fun clearSearchHistory()
}
