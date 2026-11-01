package com.audiobookbay.app.data.remote

import com.audiobookbay.app.domain.model.Audiobook
import com.audiobookbay.app.domain.model.AudiobookDetail
import com.audiobookbay.app.domain.model.AudiobookSource
import com.audiobookbay.app.domain.model.Category
import com.audiobookbay.app.domain.model.Resource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of AudiobookRemoteDataSource that delegates to
 * AudioBookBayParser with fallback to LibriVox public domain catalog.
 */
@Singleton
class AudiobookRemoteDataSourceImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val parser: AudioBookBayParser,
    private val libriVoxApi: LibriVoxApi,
    private val ioDispatcher: CoroutineDispatcher,
) : AudiobookRemoteDataSource {

    private var currentBaseUrl = "https://audiobookbay.lu"

    fun setBaseUrl(url: String) {
        currentBaseUrl = url.trimEnd('/')
    }

    override suspend fun getRecentAudiobooks(page: Int): Resource<List<Audiobook>> = withContext(ioDispatcher) {
        try {
            val url = if (page <= 1) currentBaseUrl else "$currentBaseUrl/page/$page/"
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val html = response.body?.string().orEmpty()
                val parsed = parser.parseAudiobookList(html, currentBaseUrl)
                if (parsed.isNotEmpty()) {
                    return@withContext Resource.Success(parsed)
                }
            }

            // Fallback to LibriVox API if website scraping is unavailable or blocked
            Timber.d("Falling back to LibriVox API for page $page")
            val librivoxData = libriVoxApi.getAudiobooks(limit = 20, offset = (page - 1) * 20)
            val fallbackBooks = librivoxData.books.map { dto ->
                Audiobook(
                    id = dto.id,
                    title = dto.title,
                    author = dto.authors.firstOrNull()?.let { "${it.firstName.orEmpty()} ${it.lastName.orEmpty()}".trim() } ?: "Unknown",
                    formattedSize = dto.totalTime ?: "",
                    source = AudiobookSource.LIBRIVOX,
                )
            }
            Resource.Success(fallbackBooks)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load recent audiobooks")
            Resource.Error(e.message ?: "Network error occurred", e)
        }
    }

    override suspend fun searchAudiobooks(query: String, page: Int): Resource<List<Audiobook>> = withContext(ioDispatcher) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = if (page <= 1) "$currentBaseUrl/?s=$encodedQuery" else "$currentBaseUrl/page/$page/?s=$encodedQuery"
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val html = response.body?.string().orEmpty()
                val parsed = parser.parseAudiobookList(html, currentBaseUrl)
                if (parsed.isNotEmpty()) {
                    return@withContext Resource.Success(parsed)
                }
            }

            // Fallback to LibriVox search
            val librivoxData = libriVoxApi.searchAudiobooks(title = query, limit = 20, offset = (page - 1) * 20)
            val fallbackBooks = librivoxData.books.map { dto ->
                Audiobook(
                    id = dto.id,
                    title = dto.title,
                    author = dto.authors.firstOrNull()?.let { "${it.firstName.orEmpty()} ${it.lastName.orEmpty()}".trim() } ?: "Unknown",
                    source = AudiobookSource.LIBRIVOX,
                )
            }
            Resource.Success(fallbackBooks)
        } catch (e: Exception) {
            Timber.e(e, "Search failed for query: $query")
            Resource.Error(e.message ?: "Search failed", e)
        }
    }

    override suspend fun getAudiobookDetail(idOrSlug: String): Resource<AudiobookDetail> = withContext(ioDispatcher) {
        try {
            val url = "$currentBaseUrl/audio/$idOrSlug/"
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val html = response.body?.string().orEmpty()
                val parsed = parser.parseAudiobookDetail(html, idOrSlug, currentBaseUrl)
                return@withContext Resource.Success(parsed)
            }
            Resource.Error("Audiobook details not found (HTTP ${response.code})")
        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve details for: $idOrSlug")
            Resource.Error(e.message ?: "Failed to load details", e)
        }
    }

    override suspend fun getAudiobooksByCategory(categorySlug: String, page: Int): Resource<List<Audiobook>> = withContext(ioDispatcher) {
        try {
            val url = if (page <= 1) {
                "$currentBaseUrl/audio-books/$categorySlug/"
            } else {
                "$currentBaseUrl/audio-books/$categorySlug/page/$page/"
            }
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val html = response.body?.string().orEmpty()
                val parsed = parser.parseAudiobookList(html, currentBaseUrl)
                return@withContext Resource.Success(parsed)
            }
            Resource.Error("Category listings not found (HTTP ${response.code})")
        } catch (e: Exception) {
            Timber.e(e, "Failed to get category books: $categorySlug")
            Resource.Error(e.message ?: "Failed to load category", e)
        }
    }

    override suspend fun getCategories(): Resource<List<Category>> = withContext(ioDispatcher) {
        // Predefined list of standard AudioBookBay categories verified from research
        val standardCategories = listOf(
            Category(slug = "sci-fi", displayName = "Science Fiction"),
            Category(slug = "fantasy", displayName = "Fantasy"),
            Category(slug = "mystery", displayName = "Mystery"),
            Category(slug = "thriller", displayName = "Thriller & Suspense"),
            Category(slug = "biographies-memoirs", displayName = "Biographies & Memoirs"),
            Category(slug = "history", displayName = "History"),
            Category(slug = "horror", displayName = "Horror"),
            Category(slug = "business", displayName = "Business & Economics"),
            Category(slug = "self-help", displayName = "Self Help & Motivation"),
            Category(slug = "action-adventure", displayName = "Action & Adventure"),
            Category(slug = "children", displayName = "Children's Audiobooks"),
            Category(slug = "romance", displayName = "Romance"),
        )
        Resource.Success(standardCategories)
    }
}
