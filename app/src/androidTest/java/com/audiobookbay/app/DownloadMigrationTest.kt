package com.audiobookbay.app

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.audiobookbay.app.data.local.AppDatabase
import com.audiobookbay.app.data.local.entity.CatalogCacheEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadMigrationTest {
    @Test fun versionTwoDownloadsAndLegacyTablesSurviveUpgrade() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "download-migration-test.db"
        context.deleteDatabase(name)
        var database = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            database.catalogCacheDao().put(CatalogCacheEntity("keep", "retained"))
            database.close()
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use { old ->
                old.execSQL("DROP TABLE download_history")
                old.execSQL("DROP TABLE downloads")
                old.execSQL("""CREATE TABLE downloads (id TEXT NOT NULL PRIMARY KEY, audiobookId TEXT NOT NULL, title TEXT NOT NULL, infoHash TEXT NOT NULL, destinationPath TEXT NOT NULL, status TEXT NOT NULL, progress REAL NOT NULL, bytesDownloaded INTEGER NOT NULL, totalBytes INTEGER NOT NULL, downloadSpeedBytesPerSec INTEGER NOT NULL, uploadSpeedBytesPerSec INTEGER NOT NULL, seeders INTEGER NOT NULL, peers INTEGER NOT NULL, etaSeconds INTEGER NOT NULL, errorMessage TEXT, addedTimestamp INTEGER NOT NULL, completedTimestamp INTEGER)""")
                old.execSQL("INSERT INTO downloads VALUES ('legacy', 'book', 'Legacy Book', 'legacy', '/retained/staging', 'PAUSED', 0.5, 100, 200, 0, 0, 0, 0, -1, NULL, 1, NULL)")
                old.version = 2
            }
            database = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5).build()
            val item = checkNotNull(database.downloadDao().getDownloadById("legacy"))
            assertEquals("Legacy Book", item.title)
            assertEquals("/retained/staging", item.destinationPath)
            assertNull(item.author)
            assertNull(item.destinationTreeUri)
            assertEquals(0f, item.publicationProgress)
            assertEquals("retained", database.catalogCacheDao().get("keep")?.payload)
            database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM chapters").use { assertTrue(it.moveToFirst()) }
            database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM bookmarks").use { assertTrue(it.moveToFirst()) }
        } finally { database.close(); context.deleteDatabase(name) }
    }
}
