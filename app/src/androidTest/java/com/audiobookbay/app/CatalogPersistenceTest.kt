package com.audiobookbay.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.audiobookbay.app.data.local.AppDatabase
import com.audiobookbay.app.data.remote.AudiobookRemoteDataSource
import com.audiobookbay.app.data.repository.AudiobookRepositoryImpl
import com.audiobookbay.app.domain.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogPersistenceTest {
    @Test fun cachedDetailsSurviveDatabaseReopenWithoutNetwork() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "catalog-persistence-test.db"
        context.deleteDatabase(name)
        val detail = AudiobookDetail("librivox:test", "Public domain test", "Test author", description = "Cached synopsis")
        var online = true
        val remote = object : AudiobookRemoteDataSource {
            override suspend fun getAudiobookDetail(idOrSlug: String): Resource<AudiobookDetail> =
                if (online) Resource.Success(detail) else Resource.Error("Offline")
            override suspend fun getRecentAudiobooks(page: Int): Resource<List<Audiobook>> = Resource.Error("Offline")
            override suspend fun searchAudiobooks(query: String, page: Int): Resource<List<Audiobook>> = Resource.Error("Offline")
            override suspend fun getAudiobooksByCategory(categorySlug: String, page: Int): Resource<List<Audiobook>> = Resource.Error("Offline")
            override suspend fun getCategories(): Resource<List<Category>> = Resource.Error("Offline")
        }
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        var db = open()
        try {
            val repository = AudiobookRepositoryImpl(remote, db.audiobookDao(), db.catalogCacheDao(), Json)
            assertEquals(Resource.Success(detail), repository.getAudiobookDetail(detail.id))
            db.close()
            online = false
            db = open()
            val offlineRepository = AudiobookRepositoryImpl(remote, db.audiobookDao(), db.catalogCacheDao(), Json)
            assertEquals(Resource.Success(detail), offlineRepository.getAudiobookDetail(detail.id))
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
