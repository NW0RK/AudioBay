package com.audiobookbay.app
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.audiobookbay.app.data.local.AppDatabase
import com.audiobookbay.app.data.repository.SavedBooksRepositoryImpl
import com.audiobookbay.app.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import com.audiobookbay.app.data.local.entity.SavedBookEntity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class SavedPersistenceTest {
    @Test fun migrateV3AndReopenSavedOffline() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "saved-upgrade-test.db"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5).build()
        var db = open()
        try {
            val sql = db.openHelper.writableDatabase
            sql.execSQL("INSERT INTO catalog_cache(cacheKey, payload) VALUES ('retained', 'payload')")
            sql.execSQL("DROP TABLE saved_books")
            sql.execSQL("DELETE FROM room_master_table")
            sql.version = 3
            db.close()
            db = open()
            assertEquals("payload", db.catalogCacheDao().get("retained")?.payload)
            val repo = SavedBooksRepositoryImpl(db.savedBookDao(), Json, Dispatchers.IO)
            val book = Audiobook("offline", "Saved title", "Author", language = "French")
            assertTrue(repo.setSaved(book, true) is Resource.Success)
            repo.setSaved(book, true)
            val newer = book.copy(id = "newer", title = "Newer")
            db.savedBookDao().insert(SavedBookEntity(newer.source.name, newer.id, Json.encodeToString(newer), Long.MAX_VALUE))
            db.close()
            db = open()
            val reopened = SavedBooksRepositoryImpl(db.savedBookDao(), Json, Dispatchers.IO)
            assertEquals(listOf(newer, book), (reopened.observeSaved().first() as Resource.Success).data)
            reopened.setSaved(newer, false)
            reopened.setSaved(book, false)
            assertTrue((reopened.observeSaved().first() as Resource.Success).data.isEmpty())
            assertEquals("payload", db.catalogCacheDao().get("retained")?.payload)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
