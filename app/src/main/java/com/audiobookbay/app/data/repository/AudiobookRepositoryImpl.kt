package com.audiobookbay.app.data.repository

import com.audiobookbay.app.data.local.dao.CatalogCacheDao
import com.audiobookbay.app.data.local.entity.CatalogCacheEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CancellationException
import timber.log.Timber
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
    private val cache: CatalogCacheDao,
    private val json: Json,
) : AudiobookRepository {

    private var offlineCatalog = false

    override suspend fun getRecentAudiobooks(page: Int): Resource<List<Audiobook>> {
        if (page == 1) offlineCatalog = false
        if (offlineCatalog) return readCached("recent:$page") ?: Resource.Success(emptyList())
        val result = remoteDataSource.getRecentAudiobooks(page)
        try {
            if (result is Resource.Success) {
                if (page == 1) cache.deleteMatching("recent:%")
                cache.put(CatalogCacheEntity("recent:$page", json.encodeToString(result.data)))
            } else if (result is Resource.Error && page == 1) {
                readCached<List<Audiobook>>("recent:1")?.let {
                    offlineCatalog = true
                    return it
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Catalog cache unavailable")
        }
        return result
    }

    private suspend inline fun <reified T> readCached(key: String): Resource.Success<T>? = try {
        cache.get(key)?.let { Resource.Success(json.decodeFromString<T>(it.payload)) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "Catalog snapshot unavailable")
        null
    }


    override suspend fun getAudiobookDetail(audiobookId: String): Resource<AudiobookDetail> {
        return cached("detail:$audiobookId") { remoteDataSource.getAudiobookDetail(audiobookId) }
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
    private suspend inline fun <reified T> cached(key: String, fetch: () -> Resource<T>): Resource<T> {
        val result = fetch()
        try {
            when (result) {
                is Resource.Success -> cache.put(CatalogCacheEntity(key, json.encodeToString(result.data)))
                is Resource.Error -> cache.get(key)?.let { return Resource.Success(json.decodeFromString<T>(it.payload)) }
                Resource.Loading -> Unit
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Catalog cache unavailable")
        }
        return result
    }
}
