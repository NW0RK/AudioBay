package com.audiobookbay.app.data.local

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
        AudiobookEntity::class,
        ChapterEntity::class,
        PlaybackProgressEntity::class,
        DownloadEntity::class,
        BookmarkEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audiobookDao(): AudiobookDao
    abstract fun playbackProgressDao(): PlaybackProgressDao
    abstract fun downloadDao(): DownloadDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        const val DATABASE_NAME = "audiobookbay.db"
    }
}
