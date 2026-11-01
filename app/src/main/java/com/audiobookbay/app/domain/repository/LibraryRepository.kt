package com.audiobookbay.app.domain.repository

import com.audiobookbay.app.domain.model.Audiobook
import com.audiobookbay.app.domain.model.AudiobookDetail
import com.audiobookbay.app.domain.model.Chapter
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Contract for managing downloaded and locally imported audiobooks in the user's library.
 */
interface LibraryRepository {

    /**
     * Observes all audiobooks available in the local library.
     */
    fun observeLibraryAudiobooks(): Flow<List<Audiobook>>

    /**
     * Retrieves an audiobook from the local library by ID.
     */
    suspend fun getLibraryAudiobook(audiobookId: String): AudiobookDetail?

    /**
     * Adds or updates an audiobook in the local library upon download completion.
     */
    suspend fun saveToLibrary(audiobook: AudiobookDetail, downloadedFiles: List<File>)

    /**
     * Removes an audiobook from the library, optionally deleting physical audio files.
     */
    suspend fun removeFromLibrary(audiobookId: String, deleteFiles: Boolean = true): Result<Unit>

    /**
     * Scans local directory for automatically discovering existing downloaded audiobooks (.mp3, .m4a, .m4b).
     */
    suspend fun scanLocalDirectory(directory: File): Result<List<Audiobook>>

    /**
     * Retrieves chapters for an audiobook in the library.
     */
    suspend fun getChapters(audiobookId: String): List<Chapter>

    /**
     * Returns total disk space used by downloaded audiobooks in bytes.
     */
    suspend fun calculateStorageUsageBytes(): Long
}
