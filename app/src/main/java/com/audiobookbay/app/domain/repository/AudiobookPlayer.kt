package com.audiobookbay.app.domain.repository

import com.audiobookbay.app.domain.model.Chapter
import com.audiobookbay.app.domain.model.PlaybackState
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Architectural contract for media playback, decoupling ExoPlayer/Media3 specifics from the UI.
 */
interface AudiobookPlayer {

    /**
     * Observes real-time playback state (position, buffering, playing, speed).
     */
    val playbackState: Flow<PlaybackState>

    /**
     * Prepares and starts playback of an audiobook from local files.
     *
     * @param audiobookId The unique book identifier
     * @param chapters Ordered list of chapters/audio files to queue
     * @param startChapterIndex Initial chapter index (0-based)
     * @param startPositionMs Initial seek position within the starting chapter
     */
    suspend fun playAudiobook(
        audiobookId: String,
        chapters: List<Chapter>,
        startChapterIndex: Int = 0,
        startPositionMs: Long = 0L,
    )

    /**
     * Plays or resumes playback.
     */
    fun play()

    /**
     * Pauses playback.
     */
    fun pause()

    /**
     * Toggles between play and pause.
     */
    fun togglePlayPause()

    /**
     * Seeks to an absolute position within the current chapter.
     */
    fun seekTo(positionMs: Long)

    /**
     * Relative skip forward (e.g. 30 seconds).
     */
    fun skipForward(intervalMs: Long = 30_000L)

    /**
     * Relative rewind backward (e.g. 15 seconds).
     */
    fun rewindBackward(intervalMs: Long = 15_000L)

    /**
     * Skips to the next chapter.
     */
    fun nextChapter()

    /**
     * Returns to the previous chapter.
     */
    fun previousChapter()

    /**
     * Sets playback speed multiplier (e.g. 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).
     */
    fun setPlaybackSpeed(speed: Float)

    /**
     * Sets a sleep timer to pause playback after [durationMinutes]. Pass 0 or null to cancel.
     */
    fun setSleepTimer(durationMinutes: Int?)

    /**
     * Sets a sleep timer to pause at the end of the current chapter.
     */
    fun setSleepTimerEndOfChapter()

    /**
     * Releases player resources when done.
     */
    fun release()
}
