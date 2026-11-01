package com.audiobookbay.app

import com.audiobookbay.app.data.remote.*
import com.audiobookbay.app.domain.model.*
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class DiscoveryRemoteTest {
    private val api = mockk<LibriVoxApi>()
    private val fallback = LibriVoxResponse(listOf(LibriVoxBookDto("42", "A Book", coverArt = "https://example.org/cover.jpg")))
    private fun client(code: Int = 403, fail: Boolean = false) = OkHttpClient.Builder().addInterceptor { chain ->
        if (fail) throw IOException("Unavailable")
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
            .code(code).message("Test response").body("".toResponseBody()).build()
    }.build()

    @Test fun `blocked catalog falls back and stays on same source for pagination`() = runTest {
        coEvery { api.getAudiobooks(offset = 0) } returns fallback
        coEvery { api.getAudiobooks(offset = 20) } returns LibriVoxResponse()
        val source = AudiobookRemoteDataSourceImpl(client(), AudioBookBayParser(), api, StandardTestDispatcher(testScheduler))
        val result = source.getRecentAudiobooks(1) as Resource.Success
        assertEquals("librivox:42", result.data.single().id)
        assertEquals("https://example.org/cover.jpg", result.data.single().coverImageUrl)
        assertEquals(AudiobookSource.LIBRIVOX, result.data.single().source)
        assertTrue((source.getRecentAudiobooks(2) as Resource.Success).data.isEmpty())
        coVerify(exactly = 1) { api.getAudiobooks(offset = 20) }
    }

    @Test fun `connection failure also falls back`() = runTest {
        coEvery { api.getAudiobooks(offset = 0) } returns fallback
        val source = AudiobookRemoteDataSourceImpl(client(fail = true), AudioBookBayParser(), api, StandardTestDispatcher(testScheduler))
        assertTrue(source.getRecentAudiobooks(1) is Resource.Success)
    }

    @Test fun `fallback failure becomes recoverable error`() = runTest {
        coEvery { api.getAudiobooks(offset = 0) } throws IOException("Offline")
        val source = AudiobookRemoteDataSourceImpl(client(), AudioBookBayParser(), api, StandardTestDispatcher(testScheduler))
        assertTrue(source.getRecentAudiobooks(1) is Resource.Error)
    }

    @Test fun `cancellation is never converted to network error`() = runTest {
        coEvery { api.getAudiobooks(offset = 0) } throws CancellationException("Cancelled")
        val source = AudiobookRemoteDataSourceImpl(client(), AudioBookBayParser(), api, StandardTestDispatcher(testScheduler))
        try {
            source.getRecentAudiobooks(1)
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
    }

    @Test fun `API decodes url_other arrays and numeric strings`() {
        val dto = Json { ignoreUnknownKeys = true }.decodeFromString<LibriVoxResponse>("""{"books":[{"id":"42","title":"Book","totaltimesecs":"120","url_other":[],"coverart_jpg":"https://example.org/cover.jpg"}]}""").books.single()
        assertEquals(120L, dto.totalTimeSecs)
        assertEquals("https://example.org/cover.jpg", dto.coverArt)
    }

    @Test fun `search falls back when catalog ignores query`() = runTest {
        coEvery { api.searchAudiobooks(title = "^Alice", offset = 0) } returns fallback
        val ignoredSearch = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("""<div class="post"><div class="postTitle"><h2><a href="/abss/unrelated/">Unrelated Story</a></h2></div></div>""".toResponseBody()).build()
        }.build()
        val source = AudiobookRemoteDataSourceImpl(ignoredSearch, AudioBookBayParser(), api, StandardTestDispatcher(testScheduler))
        val result = source.searchAudiobooks("Alice", 1) as Resource.Success
        assertEquals(AudiobookSource.LIBRIVOX, result.data.single().source)
    }

    @Test fun `no-match API response is an empty search not a network error`() = runTest {
        coEvery { api.searchAudiobooks(title = "^Missing", offset = 0) } throws
            retrofit2.HttpException(retrofit2.Response.error<LibriVoxResponse>(404, "No books found".toResponseBody()))
        val source = AudiobookRemoteDataSourceImpl(client(), AudioBookBayParser(), api, StandardTestDispatcher(testScheduler))
        assertEquals(Resource.Success(emptyList<Audiobook>()), source.searchAudiobooks("Missing", 1))
    }

    private fun searchClient(body: String, onRequest: (HttpUrl) -> Unit = {}) = OkHttpClient.Builder().addInterceptor { chain ->
        onRequest(chain.request().url)
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(body.toResponseBody()).build()
    }.build()

    @Test fun `mixed case and repeated spaces become a lowercase catalog query`() = runTest {
        val client = searchClient("""<link rel="canonical" href="/?s=the+last+wish" />
            <div class="post"><div class="postTitle"><h2><a href="/abss/a-story/">The Last Wish</a></h2></div></div>""") {
            assertEquals("the last wish", it.queryParameter("s"))
        }
        val source = AudiobookRemoteDataSourceImpl(client, AudioBookBayParser(), api, StandardTestDispatcher(testScheduler))
        val result = source.searchAudiobooks("  The   Last Wish  ", 1) as Resource.Success
        assertEquals("The Last Wish", result.data.single().title)
        assertEquals(AudiobookSource.AUDIOBOOKBAY, result.data.single().source)
        coVerify(exactly = 0) { api.searchAudiobooks(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test fun `verified catalog matches are retained even when keyword only appears in metadata`() = runTest {
        val html = """<link rel="canonical" href="/?s=witcher" />
            <div class="post"><div class="postTitle"><h2><a href="/abss/short-stories/">Fantasy Short Stories</a></h2></div></div>
            <div class="post"><div class="postTitle"><h2><a href="/abss/series/">The Witcher</a></h2></div></div>"""
        val source = AudiobookRemoteDataSourceImpl(searchClient(html), AudioBookBayParser(), api, StandardTestDispatcher(testScheduler))
        val result = source.searchAudiobooks("Witcher", 1) as Resource.Success
        assertEquals(listOf("The Witcher", "Fantasy Short Stories"), result.data.map { it.title })
    }

    @Test fun `empty verified catalog page does not silently switch to a different catalog`() = runTest {
        val source = AudiobookRemoteDataSourceImpl(searchClient("""<link rel="canonical" href="/?s=missing" />"""), AudioBookBayParser(), api, StandardTestDispatcher(testScheduler))
        assertEquals(Resource.Success(emptyList<Audiobook>()), source.searchAudiobooks("Missing", 1))
        coVerify(exactly = 0) { api.searchAudiobooks(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test fun `pagination uses the normalized query and keeps metadata matches accessible`() = runTest {
        val client = searchClient("""<link rel="canonical" href="/page/2/?s=witcher" />
            <div class="post"><div class="postTitle"><h2><a href="/abss/story/">A Series Story</a></h2></div></div>""") {
            assertEquals("/page/2/", it.encodedPath)
            assertEquals("witcher", it.queryParameter("s"))
        }
        val source = AudiobookRemoteDataSourceImpl(client, AudioBookBayParser(), api, StandardTestDispatcher(testScheduler))
        assertEquals("A Series Story", (source.searchAudiobooks("Witcher", 2) as Resource.Success).data.single().title)
    }
}
