package com.audiobookbay.app.data.repository

import com.audiobookbay.app.data.local.dao.AudiobookDao
import com.audiobookbay.app.data.local.entity.AudiobookEntity
import com.audiobookbay.app.data.local.entity.ChapterEntity
import com.audiobookbay.app.domain.model.Audiobook
import com.audiobookbay.app.domain.model.AudiobookDetail
import com.audiobookbay.app.domain.model.Chapter
import com.audiobookbay.app.domain.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val audiobookDao: AudiobookDao,
) : LibraryRepository {

    override fun observeLibraryAudiobooks(): Flow<List<Audiobook>> {
        return audiobookDao.observeLibraryAudiobooks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getLibraryAudiobook(audiobookId: String): AudiobookDetail? {
        val entity = audiobookDao.getAudiobookById(audiobookId) ?: return null
        val chapters = audiobookDao.getChaptersForAudiobook(audiobookId).map { it.toDomain() }
        return AudiobookDetail(
            id = entity.id,
            title = entity.title,
            author = entity.author,
            narrator = entity.narrator,
            coverImageUrl = entity.coverImageUrl,
            categories = listOfNotNull(entity.category),
            description = entity.description ?: "",
            chapters = chapters,
        )
    }

    override suspend fun saveToLibrary(audiobook: AudiobookDetail, downloadedFiles: List<File>) {
        val entity = AudiobookEntity(
            id = audiobook.id,
            title = audiobook.title,
            author = audiobook.author,
            narrator = audiobook.narrator,
            coverImageUrl = audiobook.coverImageUrl,
            category = audiobook.categories.firstOrNull(),
            durationSeconds = audiobook.durationSeconds,
            formattedSize = audiobook.formattedSize,
            detailUrl = null,
            infoHash = audiobook.torrentInfo?.infoHash,
            source = audiobook.source.name,
            description = audiobook.description,
            isDownloaded = true,
            localDirectoryPath = downloadedFiles.firstOrNull()?.parentFile?.absolutePath,
        )
        audiobookDao.upsertAudiobook(entity)

        val chapterEntities = downloadedFiles.mapIndexed { index, file ->
            ChapterEntity(
                id = "${audiobook.id}_ch_$index",
                audiobookId = audiobook.id,
                title = file.nameWithoutExtension,
                trackNumber = index + 1,
                filePath = file.absolutePath,
                fileSizeBytes = file.length(),
            )
        }
        audiobookDao.upsertChapters(chapterEntities)
    }

    override suspend fun removeFromLibrary(audiobookId: String, deleteFiles: Boolean): Result<Unit> = runCatching {
        if (deleteFiles) {
            val chapters = audiobookDao.getChaptersForAudiobook(audiobookId)
            chapters.forEach { ch ->
                ch.filePath?.let { path ->
                    File(path).delete()
                }
            }
        }
        audiobookDao.deleteAudiobookById(audiobookId)
    }

    override suspend fun scanLocalDirectory(directory: File): Result<List<Audiobook>> = runCatching {
        withContext(Dispatchers.IO) {
            val audioExtensions = setOf("mp3", "m4a", "m4b", "flac", "ogg")
            val audioFiles = directory.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in audioExtensions }
                .toList()

            // Group by parent folder (assuming 1 folder per audiobook)
            val groupedByDir = audioFiles.groupBy { it.parentFile ?: directory }
            groupedByDir.map { (dir, files) ->
                Audiobook(
                    id = dir.absolutePath.hashCode().toString(),
                    title = dir.name,
                    author = "Local Audio",
                    formattedSize = "${files.sumOf { it.length() } / (1024 * 1024)} MB",
                    source = com.audiobookbay.app.domain.model.AudiobookSource.LOCAL_DEVICE,
                )
            }
        }
    }

    override suspend fun getChapters(audiobookId: String): List<Chapter> {
        return audiobookDao.getChaptersForAudiobook(audiobookId).map { it.toDomain() }
    }

    override suspend fun calculateStorageUsageBytes(): Long = withContext(Dispatchers.IO) {
        // Sum sizes of files in library
        0L // Codex can refine with actual directory scan
    }
}
