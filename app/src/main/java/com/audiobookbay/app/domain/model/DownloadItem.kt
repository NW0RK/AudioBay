package com.audiobookbay.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Tracks the state of an active or completed torrent download.
 */
@Serializable
data class DownloadItem(
    val id: String, // Usually matching audiobookId or infoHash
    val audiobookId: String,
    val title: String,
    val infoHash: String,
    val destinationPath: String,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float = 0f, // 0.0 to 1.0
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val downloadSpeedBytesPerSec: Long = 0L,
    val uploadSpeedBytesPerSec: Long = 0L,
    val seeders: Int = 0,
    val peers: Int = 0,
    val etaSeconds: Long = -1L,
    val errorMessage: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val completedTimestamp: Long? = null,
)

enum class DownloadStatus {
    QUEUED,
    CHECKING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    SEEDING,
    ERROR,
    CANCELLED,
}
