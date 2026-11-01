package com.audiobookbay.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.audiobookbay.app.data.local.entity.AudiobookEntity
import com.audiobookbay.app.data.local.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudiobookDao {

    @Query("SELECT * FROM audiobooks ORDER BY addedAtTimestamp DESC")
    fun observeAllAudiobooks(): Flow<List<AudiobookEntity>>

    @Query("SELECT * FROM audiobooks WHERE isDownloaded = 1 ORDER BY addedAtTimestamp DESC")
    fun observeLibraryAudiobooks(): Flow<List<AudiobookEntity>>

    @Query("SELECT * FROM audiobooks WHERE id = :id LIMIT 1")
    suspend fun getAudiobookById(id: String): AudiobookEntity?

    @Upsert
    suspend fun upsertAudiobook(audiobook: AudiobookEntity)

    @Upsert
    suspend fun upsertAudiobooks(audiobooks: List<AudiobookEntity>)

    @Query("DELETE FROM audiobooks WHERE id = :id")
    suspend fun deleteAudiobookById(id: String)

    @Query("SELECT * FROM chapters WHERE audiobookId = :audiobookId ORDER BY trackNumber ASC")
    suspend fun getChaptersForAudiobook(audiobookId: String): List<ChapterEntity>

    @Upsert
    suspend fun upsertChapters(chapters: List<ChapterEntity>)

    @Query("DELETE FROM chapters WHERE audiobookId = :audiobookId")
    suspend fun deleteChaptersForAudiobook(audiobookId: String)
}
