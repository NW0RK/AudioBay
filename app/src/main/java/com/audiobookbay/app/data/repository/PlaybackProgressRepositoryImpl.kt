package com.audiobookbay.app.data.repository

import com.audiobookbay.app.data.local.dao.BookmarkDao
import com.audiobookbay.app.data.local.dao.PlaybackProgressDao
import com.audiobookbay.app.data.local.entity.BookmarkEntity
import com.audiobookbay.app.data.local.entity.PlaybackProgressEntity
import com.audiobookbay.app.domain.model.Bookmark
import com.audiobookbay.app.domain.model.PlaybackProgress
import com.audiobookbay.app.domain.repository.PlaybackProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackProgressRepositoryImpl @Inject constructor(
    private val progressDao: PlaybackProgressDao,
    private val bookmarkDao: BookmarkDao,
) : PlaybackProgressRepository {

    override fun observeProgress(audiobookId: String): Flow<PlaybackProgress?> {
        return progressDao.observeProgress(audiobookId).map { it?.toDomain() }
    }

    override suspend fun getProgress(audiobookId: String): PlaybackProgress? {
        return progressDao.getProgress(audiobookId)?.toDomain()
    }

    override suspend fun saveProgress(
        audiobookId: String,
        positionMs: Long,
        totalDurationMs: Long,
        chapterIndex: Int,
        isCompleted: Boolean,
    ) {
        val entity = PlaybackProgressEntity(
            audiobookId = audiobookId,
            currentPositionMs = positionMs,
            totalDurationMs = totalDurationMs,
            currentChapterIndex = chapterIndex,
            isCompleted = isCompleted,
            lastListenedTimestamp = System.currentTimeMillis(),
        )
        progressDao.upsertProgress(entity)
    }

    override suspend fun markAsCompleted(audiobookId: String) {
        progressDao.markCompleted(audiobookId)
    }

    override suspend fun resetProgress(audiobookId: String) {
        progressDao.upsertProgress(
            PlaybackProgressEntity(
                audiobookId = audiobookId,
                currentPositionMs = 0L,
                totalDurationMs = 0L,
                currentChapterIndex = 0,
                isCompleted = false,
                lastListenedTimestamp = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun addBookmark(bookmark: Bookmark) {
        bookmarkDao.upsertBookmark(
            BookmarkEntity(
                id = bookmark.id,
                audiobookId = bookmark.audiobookId,
                chapterIndex = bookmark.chapterIndex,
                positionMs = bookmark.positionMs,
                label = bookmark.label,
                note = bookmark.note,
                createdAtTimestamp = bookmark.createdAtTimestamp,
            ),
        )
    }

    override fun observeBookmarks(audiobookId: String): Flow<List<Bookmark>> {
        return bookmarkDao.observeBookmarksForAudiobook(audiobookId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun deleteBookmark(bookmarkId: String) {
        bookmarkDao.deleteBookmark(bookmarkId)
    }
}
