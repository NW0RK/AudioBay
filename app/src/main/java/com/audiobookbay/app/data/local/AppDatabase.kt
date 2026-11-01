package com.audiobookbay.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.audiobookbay.app.data.local.dao.CatalogCacheDao
import com.audiobookbay.app.data.local.entity.CatalogCacheEntity
import androidx.room.Database
import androidx.room.RoomDatabase
import com.audiobookbay.app.data.local.dao.AudiobookDao
import com.audiobookbay.app.data.local.dao.BookmarkDao
import com.audiobookbay.app.data.local.dao.DownloadDao
import com.audiobookbay.app.data.local.dao.PlaybackProgressDao
import com.audiobookbay.app.data.local.entity.AudiobookEntity
import com.audiobookbay.app.data.local.entity.BookmarkEntity
import com.audiobookbay.app.data.local.entity.ChapterEntity
import com.audiobookbay.app.data.local.entity.DownloadEntity
import com.audiobookbay.app.data.local.entity.PlaybackProgressEntity

@Database(
    entities = [
        com.audiobookbay.app.data.local.entity.DownloadHistoryEntity::class,
        com.audiobookbay.app.data.local.entity.SavedBookEntity::class,
        AudiobookEntity::class,
        CatalogCacheEntity::class,
        ChapterEntity::class,
        PlaybackProgressEntity::class,
        DownloadEntity::class,
        BookmarkEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedBookDao(): com.audiobookbay.app.data.local.dao.SavedBookDao
    abstract fun catalogCacheDao(): CatalogCacheDao
    abstract fun audiobookDao(): AudiobookDao
    abstract fun playbackProgressDao(): PlaybackProgressDao
    abstract fun downloadDao(): DownloadDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS download_history (infoHash TEXT NOT NULL PRIMARY KEY, audiobookId TEXT NOT NULL, title TEXT NOT NULL, author TEXT NOT NULL, completedAt INTEGER NOT NULL)")
                db.execSQL("INSERT OR IGNORE INTO download_history SELECT infoHash, audiobookId, title, author, completedTimestamp FROM downloads WHERE status = 'COMPLETED' AND publishedFolderUri IS NOT NULL AND completedTimestamp IS NOT NULL AND trim(title) != '' AND author IS NOT NULL AND trim(author) != ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `saved_books` (`source` TEXT NOT NULL, `bookId` TEXT NOT NULL, `payload` TEXT NOT NULL, `savedAt` INTEGER NOT NULL, PRIMARY KEY(`source`, `bookId`))")
            }
        }
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `catalog_cache` (`cacheKey` TEXT NOT NULL, `payload` TEXT NOT NULL, PRIMARY KEY(`cacheKey`))")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloads ADD COLUMN author TEXT")
                db.execSQL("ALTER TABLE downloads ADD COLUMN destinationTreeUri TEXT")
                db.execSQL("ALTER TABLE downloads ADD COLUMN publishedFolderUri TEXT")
                db.execSQL("ALTER TABLE downloads ADD COLUMN publicationProgress REAL NOT NULL DEFAULT 0")
            }
        }
        const val DATABASE_NAME = "audiobookbay.db"
    }
}
