package com.audiobookbay.app.domain.model

import kotlinx.serialization.Serializable

/**
 * State representing active or remembered media playback.
 */
@Serializable
data class PlaybackState(
    val currentAudiobookId: String? = null,
    val currentChapterId: String? = null,
    val currentChapterIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerRemainingMs: Long? = null,
)

/**
 * Persisted listening progress for an audiobook in Room DB.
 */
@Serializable
data class PlaybackProgress(
    val audiobookId: String,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val currentChapterIndex: Int = 0,
    val isCompleted: Boolean = false,
    val lastListenedTimestamp: Long = System.currentTimeMillis(),
)
