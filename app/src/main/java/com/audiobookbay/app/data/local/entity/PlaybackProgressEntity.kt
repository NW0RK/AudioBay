package com.audiobookbay.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.audiobookbay.app.domain.model.PlaybackProgress

@Entity(
    tableName = "playback_progress",
    indices = [Index(value = ["audiobookId"], unique = true)],
)
data class PlaybackProgressEntity(
    @PrimaryKey val audiobookId: String,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val currentChapterIndex: Int = 0,
    val isCompleted: Boolean = false,
    val lastListenedTimestamp: Long = System.currentTimeMillis(),
) {
    fun toDomain(): PlaybackProgress = PlaybackProgress(
        audiobookId = audiobookId,
        currentPositionMs = currentPositionMs,
        totalDurationMs = totalDurationMs,
        currentChapterIndex = currentChapterIndex,
        isCompleted = isCompleted,
        lastListenedTimestamp = lastListenedTimestamp,
    )
}
