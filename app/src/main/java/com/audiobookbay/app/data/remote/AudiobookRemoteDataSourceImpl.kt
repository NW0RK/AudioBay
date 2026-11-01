package com.audiobookbay.app.data.remote

import com.audiobookbay.app.domain.model.Audiobook
import com.audiobookbay.app.domain.model.AudiobookDetail
import com.audiobookbay.app.domain.model.AudiobookSource
import com.audiobookbay.app.domain.model.Category
import com.audiobookbay.app.domain.model.Resource
import com.audiobookbay.app.di.IoDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AudiobookRemoteDataSource {

    private var currentBaseUrl = "https://audiobookbay.lu"

    fun setBaseUrl(url: String) {
        currentBaseUrl = url.trimEnd('/')
    }

    // Keep a paginated catalog on one source until the user refreshes.
    private var recentUsesLibriVox = false
    private var searchUsesLibriVox = false

    override suspend fun getRecentAudiobooks(page: Int): Resource<List<Audiobook>> = withContext(ioDispatcher) {
        require(page > 0)
        try {
            if (page == 1) recentUsesLibriVox = false
            if (!recentUsesLibriVox) {
                val url = if (page == 1) currentBaseUrl else "$currentBaseUrl/page/$page/"
                val books = fetchListing(url)
                if (books != null && (books.isNotEmpty() || page > 1)) {
                    return@withContext Resource.Success(books)
                }
                // Switching a partially loaded catalog would skip the fallback's first pages.
                if (page > 1) return@withContext Resource.Error("Catalog temporarily unavailable")
                recentUsesLibriVox = true
            }
            Resource.Success(libriVoxBooks { libriVoxApi.getAudiobooks(offset = (page - 1) * 20) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to load recent audiobooks")
            Resource.Error("Unable to load audiobooks", e)
        }
    }

    override suspend fun searchAudiobooks(query: String, page: Int): Resource<List<Audiobook>> = withContext(ioDispatcher) {
        require(page > 0)
        try {
            if (page == 1) searchUsesLibriVox = false
            if (!searchUsesLibriVox) {
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val url = if (page == 1) "$currentBaseUrl/?s=$encodedQuery" else "$currentBaseUrl/page/$page/?s=$encodedQuery"
                // Some catalog responses ignore the query and return the home page.
                // Never present unrelated home listings as search matches.
                val terms = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
                val books = fetchListing(url)?.filter { book ->
                    val searchable = "${book.title} ${book.author} ${book.narrator.orEmpty()}".lowercase()
                    terms.all { it in searchable }
                }
                if (books != null && (books.isNotEmpty() || page > 1)) {
                    return@withContext Resource.Success(books)
                }
                if (page > 1) return@withContext Resource.Error("Catalog temporarily unavailable")
                searchUsesLibriVox = true
            }
            Resource.Success(libriVoxBooks { libriVoxApi.searchAudiobooks(title = "^${query.trim().trimStart('^')}", offset = (page - 1) * 20) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Search failed")
            Resource.Error("Unable to search audiobooks", e)
        }
    }

    private suspend fun libriVoxBooks(fetch: suspend () -> LibriVoxResponse): List<Audiobook> = try {
        fetch().books.map { it.toAudiobook() }
    } catch (e: retrofit2.HttpException) {
        // The API uses 404 for an exhausted page or a query with no matches.
        if (e.code() == 404) emptyList() else throw e
    }

    private suspend fun fetchListing(url: String): List<Audiobook>? = try {
        okHttpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
            currentCoroutineContext().ensureActive()
            if (response.isSuccessful) parser.parseAudiobookList(response.body?.string().orEmpty(), currentBaseUrl)
            else null
        }
    } catch (e: java.io.IOException) {
        currentCoroutineContext().ensureActive()
        Timber.w(e, "Catalog unavailable; trying public-domain catalog")
        null
    }

    override suspend fun getAudiobookDetail(idOrSlug: String): Resource<AudiobookDetail> = withContext(ioDispatcher) {
        try {
            if (idOrSlug.startsWith("librivox:")) {
                val dto = libriVoxApi.getAudiobook(idOrSlug.removePrefix("librivox:")).books.firstOrNull()
                return@withContext dto?.let { Resource.Success(it.toDetail()) }
                    ?: Resource.Error("Audiobook not found")
            }
            val url = "$currentBaseUrl/abss/$idOrSlug/"
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->

            if (response.isSuccessful) {
                val html = response.body?.string().orEmpty()
                val parsed = parser.parseAudiobookDetail(html, idOrSlug, currentBaseUrl)
                return@withContext Resource.Success(parsed)
            }
            Resource.Error("Audiobook details not found (HTTP ${response.code})")
            }
        } catch (e: CancellationException) {
            throw e
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
            okHttpClient.newCall(request).execute().use { response ->

            if (response.isSuccessful) {
                val html = response.body?.string().orEmpty()
                val parsed = parser.parseAudiobookList(html, currentBaseUrl)
                return@withContext Resource.Success(parsed)
            }
            Resource.Error("Category listings not found (HTTP ${response.code})")
            }
        } catch (e: CancellationException) {
            throw e
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
