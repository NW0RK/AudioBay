package com.audiobookbay.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.audiobookbay.app.domain.model.DownloadItem
import com.audiobookbay.app.domain.model.DownloadStatus

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val audiobookId: String,
    val title: String,
    val infoHash: String,
    val destinationPath: String,
    val status: String,
    val progress: Float,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val downloadSpeedBytesPerSec: Long = 0L,
    val uploadSpeedBytesPerSec: Long = 0L,
    val seeders: Int = 0,
    val peers: Int = 0,
    val etaSeconds: Long = -1L,
    val errorMessage: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val completedTimestamp: Long? = null,
) {
    fun toDomain(): DownloadItem = DownloadItem(
        id = id,
        audiobookId = audiobookId,
        title = title,
        infoHash = infoHash,
        destinationPath = destinationPath,
        status = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.QUEUED),
        progress = progress,
        bytesDownloaded = bytesDownloaded,
        totalBytes = totalBytes,
        downloadSpeedBytesPerSec = downloadSpeedBytesPerSec,
        uploadSpeedBytesPerSec = uploadSpeedBytesPerSec,
        seeders = seeders,
        peers = peers,
        etaSeconds = etaSeconds,
        errorMessage = errorMessage,
        addedTimestamp = addedTimestamp,
        completedTimestamp = completedTimestamp,
    )
}
