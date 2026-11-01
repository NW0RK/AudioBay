package com.audiobookbay.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.audiobookbay.app.data.local.entity.PlaybackProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackProgressDao {

    @Query("SELECT * FROM playback_progress WHERE audiobookId = :audiobookId LIMIT 1")
    fun observeProgress(audiobookId: String): Flow<PlaybackProgressEntity?>

    @Query("SELECT * FROM playback_progress WHERE audiobookId = :audiobookId LIMIT 1")
    suspend fun getProgress(audiobookId: String): PlaybackProgressEntity?

    @Upsert
    suspend fun upsertProgress(progress: PlaybackProgressEntity)

    @Query("UPDATE playback_progress SET isCompleted = 1 WHERE audiobookId = :audiobookId")
    suspend fun markCompleted(audiobookId: String)

    @Query("DELETE FROM playback_progress WHERE audiobookId = :audiobookId")
    suspend fun deleteProgress(audiobookId: String)
}
