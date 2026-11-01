package com.audiobookbay.app

import com.audiobookbay.app.data.local.dao.*
import com.audiobookbay.app.data.local.entity.CatalogCacheEntity
import com.audiobookbay.app.data.remote.AudiobookRemoteDataSource
import com.audiobookbay.app.data.repository.AudiobookRepositoryImpl
import com.audiobookbay.app.domain.model.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class CatalogCacheTest {
    private val remote = mockk<AudiobookRemoteDataSource>()
    private val cache = mockk<CatalogCacheDao>(relaxed = true)
    private val dao = mockk<AudiobookDao>()
    private val repository = AudiobookRepositoryImpl(remote, dao, cache, Json)

    @Test fun `details are saved and survive network failures`() = runTest {
        val detail = AudiobookDetail("librivox:42", "A Book", "An Author", description = "Synopsis")
        coEvery { remote.getAudiobookDetail(detail.id) } returns Resource.Success(detail)
        assertEquals(Resource.Success(detail), repository.getAudiobookDetail(detail.id))
        coVerify { cache.put(CatalogCacheEntity("detail:librivox:42", Json.encodeToString(detail))) }
        coEvery { remote.getAudiobookDetail(detail.id) } returns Resource.Error("Offline")
        coEvery { cache.get("detail:librivox:42") } returns CatalogCacheEntity("detail:librivox:42", Json.encodeToString(detail))
        assertEquals(Resource.Success(detail), repository.getAudiobookDetail(detail.id))
    }

    @Test fun `offline pagination stays on cached catalog`() = runTest {
        val books = listOf(Audiobook("1", "Book", "Author"))
        coEvery { remote.getRecentAudiobooks(1) } returns Resource.Error("Offline")
        coEvery { cache.get("recent:1") } returns CatalogCacheEntity("recent:1", Json.encodeToString(books))
        coEvery { cache.get("recent:2") } returns null
        assertEquals(Resource.Success(books), repository.getRecentAudiobooks(1))
        assertEquals(Resource.Success(emptyList<Audiobook>()), repository.getRecentAudiobooks(2))
        coVerify(exactly = 0) { remote.getRecentAudiobooks(2) }
    }

    @Test fun `broken cache cannot hide successful network response`() = runTest {
        coEvery { remote.getRecentAudiobooks(1) } returns Resource.Success(emptyList())
        coEvery { cache.deleteMatching(any()) } throws IllegalStateException("Disk full")
        assertEquals(Resource.Success(emptyList<Audiobook>()), repository.getRecentAudiobooks(1))
    }
}
