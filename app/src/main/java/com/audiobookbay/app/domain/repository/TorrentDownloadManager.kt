package com.audiobookbay.app.domain.repository

import com.audiobookbay.app.domain.model.DownloadMetadata
import com.audiobookbay.app.domain.model.DownloadItem
import com.audiobookbay.app.domain.model.TorrentFile
import com.audiobookbay.app.domain.model.TorrentInfo
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Architectural contract decoupling the application from the underlying BitTorrent engine
 * (such as libtorrent4j). Any torrent implementation can be swapped cleanly behind this interface.
 */
interface TorrentDownloadManager {

    /**
     * Initializes the BitTorrent engine session, settings, and DHT listener.
     */
    suspend fun initialize()

    /**
     * Shuts down active sessions cleanly, saving fast-resume data to disk.
     */
    suspend fun shutdown()

    /**
     * Starts downloading from a magnet link or info hash.
     *
     * @param audiobookId Unique book ID to link with local library
     * @param magnetUri Full magnet URI (containing xt=urn:btih:... and optional trackers)
     * @param metadata Required author, book title and shared folder grant
     * @param selectedFiles Optional subset of files to prioritize (others ignored)
     */
    suspend fun startDownload(
        audiobookId: String,
        magnetUri: String,
        metadata: DownloadMetadata,
        selectedFiles: List<Int>? = null,
    ): Result<DownloadItem>

    /**
     * Starts downloading from a `.torrent` file.
     */
    suspend fun startDownloadFromFile(
        audiobookId: String,
        torrentFile: File,
        metadata: DownloadMetadata,
        selectedFiles: List<Int>? = null,
    ): Result<DownloadItem>

    /**
     * Pauses an active torrent download.
     */
    suspend fun updateMetadata(infoHash: String, metadata: DownloadMetadata): Result<Unit>

    suspend fun pauseDownload(infoHash: String): Result<Unit>

    /**
     * Resumes a paused torrent download.
     */
    suspend fun resumeDownload(infoHash: String): Result<Unit>

    /**
     * Cancels and removes a torrent download, optionally deleting downloaded files.
     */
    suspend fun cancelDownload(infoHash: String, deleteFiles: Boolean = false): Result<Unit>

    /**
     * Fetches metadata (file listing, size) for a magnet link before committing to download.
     */
    suspend fun fetchTorrentMetadata(magnetUri: String, timeoutMs: Long = 30_000): Result<TorrentInfo>

    /**
     * Observes real-time download status for all tracked torrents.
     */
    fun observeDownloads(): Flow<List<DownloadItem>>

    /**
     * Observes a specific download by its info hash or book ID.
     */
    fun observeDownload(infoHash: String): Flow<DownloadItem?>

    /**
     * Sets selective file priority for a torrent (skip non-audio files, samples, or unwanted tracks).
     */
    suspend fun setFilePriorities(infoHash: String, filePriorities: Map<Int, Boolean>): Result<Unit>

    /**
     * Configures network constraints (e.g. WiFi-only, upload/download limits).
     */
    suspend fun updateNetworkSettings(wifiOnly: Boolean, maxDownloadSpeedBytes: Long, maxUploadSpeedBytes: Long)
}
