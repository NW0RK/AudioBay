package com.audiobookbay.app.data.remote

import com.audiobookbay.app.domain.model.Audiobook
import com.audiobookbay.app.domain.model.AudiobookDetail
import com.audiobookbay.app.domain.model.Category
import com.audiobookbay.app.domain.model.Resource

/**
 * Common contract for fetching remote audiobook data from any backend or website.
 */
interface AudiobookRemoteDataSource {
    suspend fun getRecentAudiobooks(page: Int): Resource<List<Audiobook>>
    suspend fun searchAudiobooks(query: String, page: Int): Resource<List<Audiobook>>
    suspend fun getAudiobookDetail(idOrSlug: String): Resource<AudiobookDetail>
    suspend fun getAudiobooksByCategory(categorySlug: String, page: Int): Resource<List<Audiobook>>
    suspend fun getCategories(): Resource<List<Category>>
}
