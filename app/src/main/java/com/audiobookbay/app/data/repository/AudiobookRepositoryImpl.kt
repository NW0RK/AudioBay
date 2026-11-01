package com.audiobookbay.app.data.repository

import com.audiobookbay.app.data.local.dao.AudiobookDao
import com.audiobookbay.app.data.remote.AudiobookRemoteDataSource
import com.audiobookbay.app.domain.model.Audiobook
import com.audiobookbay.app.domain.model.AudiobookDetail
import com.audiobookbay.app.domain.model.Category
import com.audiobookbay.app.domain.model.Resource
import com.audiobookbay.app.domain.repository.AudiobookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudiobookRepositoryImpl @Inject constructor(
    private val remoteDataSource: AudiobookRemoteDataSource,
    private val audiobookDao: AudiobookDao,
) : AudiobookRepository {

    override suspend fun getRecentAudiobooks(page: Int): Resource<List<Audiobook>> {
        return remoteDataSource.getRecentAudiobooks(page)
    }

    override suspend fun getAudiobookDetail(audiobookId: String): Resource<AudiobookDetail> {
        return remoteDataSource.getAudiobookDetail(audiobookId)
    }

    override suspend fun getAudiobooksByCategory(categorySlug: String, page: Int): Resource<List<Audiobook>> {
        return remoteDataSource.getAudiobooksByCategory(categorySlug, page)
    }

    override suspend fun getCategories(): Resource<List<Category>> {
        return remoteDataSource.getCategories()
    }

    override fun observeCachedAudiobooks(): Flow<List<Audiobook>> {
        return audiobookDao.observeAllAudiobooks().map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
