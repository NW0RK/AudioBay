package com.audiobookbay.app.domain.repository

import com.audiobookbay.app.domain.model.Audiobook
import com.audiobookbay.app.domain.model.AudiobookDetail
import com.audiobookbay.app.domain.model.Category
import com.audiobookbay.app.domain.model.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Contract for discovering and retrieving audiobooks from remote and cached sources.
 */
interface AudiobookRepository {

    /**
     * Retrieves featured/recent audiobooks with pagination support.
     *
     * @param page Page index (1-based)
     */
    suspend fun getRecentAudiobooks(page: Int = 1): Resource<List<Audiobook>>

    /**
     * Retrieves full details, metadata, synopsis, and torrent info for a specific audiobook.
     *
     * @param audiobookId The unique identifier or slug of the audiobook
     */
    suspend fun getAudiobookDetail(audiobookId: String): Resource<AudiobookDetail>

    /**
     * Retrieves audiobooks belonging to a specific category/genre.
     *
     * @param categorySlug The URL slug or identifier of the category
     * @param page Page index (1-based)
     */
    suspend fun getAudiobooksByCategory(categorySlug: String, page: Int = 1): Resource<List<Audiobook>>

    /**
     * Lists available categories.
     */
    suspend fun getCategories(): Resource<List<Category>>

    /**
     * Observes cached audiobooks from local Room persistence.
     */
    fun observeCachedAudiobooks(): Flow<List<Audiobook>>
}
