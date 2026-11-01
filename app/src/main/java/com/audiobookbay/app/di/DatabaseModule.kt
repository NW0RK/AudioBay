package com.audiobookbay.app.di

import android.content.Context
import androidx.room.Room
import com.audiobookbay.app.data.local.dao.CatalogCacheDao
import com.audiobookbay.app.data.local.AppDatabase
import com.audiobookbay.app.data.local.dao.AudiobookDao
import com.audiobookbay.app.data.local.dao.BookmarkDao
import com.audiobookbay.app.data.local.dao.DownloadDao
import com.audiobookbay.app.data.local.dao.PlaybackProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideCatalogCacheDao(database: AppDatabase): CatalogCacheDao = database.catalogCacheDao()

    @Provides
    fun provideAudiobookDao(database: AppDatabase): AudiobookDao = database.audiobookDao()

    @Provides
    fun providePlaybackProgressDao(database: AppDatabase): PlaybackProgressDao = database.playbackProgressDao()

    @Provides
    fun provideDownloadDao(database: AppDatabase): DownloadDao = database.downloadDao()

    @Provides
    fun provideBookmarkDao(database: AppDatabase): BookmarkDao = database.bookmarkDao()
}
