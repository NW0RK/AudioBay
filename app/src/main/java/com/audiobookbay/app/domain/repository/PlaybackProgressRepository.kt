package com.audiobookbay.app.domain.repository

import com.audiobookbay.app.domain.model.Bookmark
import com.audiobookbay.app.domain.model.PlaybackProgress
import kotlinx.coroutines.flow.Flow

/**
 * Contract for persisting and observing listening positions, completion status, and user bookmarks.
 */
interface PlaybackProgressRepository {

    /**
     * Observes listening progress for a given audiobook.
     */
    fun observeProgress(audiobookId: String): Flow<PlaybackProgress?>

    /**
     * Retrieves listening progress synchronously/on-demand.
     */
    suspend fun getProgress(audiobookId: String): PlaybackProgress?

    /**
     * Saves or updates current listening progress (throttled during continuous playback).
     */
    suspend fun saveProgress(
        audiobookId: String,
        positionMs: Long,
        totalDurationMs: Long,
        chapterIndex: Int,
        isCompleted: Boolean = false,
    )

    /**
     * Marks an audiobook as finished/completed.
     */
    suspend fun markAsCompleted(audiobookId: String)

    /**
     * Resets progress to the beginning.
     */
    suspend fun resetProgress(audiobookId: String)

    /**
     * Adds a user bookmark at a specific position.
     */
    suspend fun addBookmark(bookmark: Bookmark)

    /**
     * Observes bookmarks for an audiobook.
     */
    fun observeBookmarks(audiobookId: String): Flow<List<Bookmark>>

    /**
     * Deletes a bookmark.
     */
    suspend fun deleteBookmark(bookmarkId: String)
}
