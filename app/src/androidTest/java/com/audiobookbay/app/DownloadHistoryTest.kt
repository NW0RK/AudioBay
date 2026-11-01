package com.audiobookbay.app
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.audiobookbay.app.data.local.AppDatabase
import com.audiobookbay.app.data.local.entity.DownloadHistoryEntity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
class DownloadHistoryTest {
    @Test fun versionFourBackfillsOnlyPublishedCompletedBooks() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "history-backfill-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            val item = com.audiobookbay.app.data.local.entity.DownloadEntity("complete", "book", "Title", "complete", "", "COMPLETED", 1f, 1, 1, author="Author", publishedFolderUri="content://published", completedTimestamp=12)
            db.downloadDao().upsertDownload(item)
            db.downloadDao().upsertDownload(item.copy(id="failed", infoHash="failed", status="PUBLICATION_FAILED"))
            db.close()
            android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE).use { old ->
                old.execSQL("DROP TABLE download_history"); old.version=4
            }
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_4_5).build()
            assertEquals(listOf("complete"), db.downloadDao().observeHistory().first().map { it.infoHash })
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun historySurvivesQueueRemovalAndDatabaseReopenAndCanBeCleared() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "history-retention-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            db.downloadDao().recordHistory(DownloadHistoryEntity("hash", "book", "წიგნი", "Émile", 1))
            db.downloadDao().deleteDownloadByInfoHash("hash")
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            assertEquals("წიგნი", db.downloadDao().observeHistory().first().single().title)
            db.downloadDao().clearHistory()
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            assertTrue(db.downloadDao().observeHistory().first().isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
