package com.audiobookbay.app.torrent

import android.content.Context
import com.audiobookbay.app.domain.model.DownloadItem
import com.audiobookbay.app.domain.model.DownloadStatus
import com.audiobookbay.app.domain.model.TorrentInfo
import com.audiobookbay.app.domain.repository.TorrentDownloadManager
import com.audiobookbay.app.torrent.service.TorrentDownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [TorrentDownloadManager] using libtorrent4j.
 *
 * NOTE FOR CODEX:
 * 1. Initialize `org.libtorrent4j.SessionManager` inside [initialize].
 * 2. Configure `SettingsPack` for bandwidth, max peer connections, and DHT.
 * 3. Add an `AlertListener` to listen to alert events:
 *    - `TorrentAddedAlert`, `TorrentFinishedAlert`, `PieceFinishedAlert`, `StateChangedAlert`.
 * 4. Call [TorrentDownloadService.start] when a download starts to keep Android background execution active.
 * 5. Handle fast-resume data via `saveResumeData()` to survive app restarts.
 */
@Singleton
class TorrentDownloadManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : TorrentDownloadManager {

    private val _downloads = MutableStateFlow<Map<String, DownloadItem>>(emptyMap())

    override suspend fun initialize() {
        Timber.i("Initializing BitTorrent engine")
        // TODO (Codex): Initialize org.libtorrent4j.SessionManager and start DHT
    }

    override suspend fun shutdown() {
        Timber.i("Shutting down BitTorrent engine")
        // TODO (Codex): Save fast-resume data and stop sessionManager
    }

    override suspend fun startDownload(
        audiobookId: String,
        magnetUri: String,
        destinationDir: File,
        selectedFiles: List<Int>?,
    ): Result<DownloadItem> = runCatching {
        TorrentDownloadService.start(context)

        // TODO (Codex): Add magnet to libtorrent4j SessionManager
        val infoHash = magnetUri.substringAfter("btih:").substringBefore("&").lowercase()
        val item = DownloadItem(
            id = infoHash,
            audiobookId = audiobookId,
            title = "Audiobook Download",
            infoHash = infoHash,
            destinationPath = destinationDir.absolutePath,
            status = DownloadStatus.DOWNLOADING,
        )
        _downloads.value = _downloads.value + (infoHash to item)
        item
    }

    override suspend fun startDownloadFromFile(
        audiobookId: String,
        torrentFile: File,
        destinationDir: File,
        selectedFiles: List<Int>?,
    ): Result<DownloadItem> = runCatching {
        TorrentDownloadService.start(context)
        // TODO (Codex): Load TorrentInfo(torrentFile) and add to sessionManager
        val item = DownloadItem(
            id = audiobookId,
            audiobookId = audiobookId,
            title = torrentFile.nameWithoutExtension,
            infoHash = "file_download",
            destinationPath = destinationDir.absolutePath,
            status = DownloadStatus.DOWNLOADING,
        )
        _downloads.value = _downloads.value + (item.infoHash to item)
        item
    }

    override suspend fun pauseDownload(infoHash: String): Result<Unit> = runCatching {
        // TODO (Codex): sessionManager.find(Sha1Hash(infoHash))?.pause()
        _downloads.value[infoHash]?.let { current ->
            _downloads.value = _downloads.value + (infoHash to current.copy(status = DownloadStatus.PAUSED))
        }
    }

    override suspend fun resumeDownload(infoHash: String): Result<Unit> = runCatching {
        // TODO (Codex): sessionManager.find(Sha1Hash(infoHash))?.resume()
        _downloads.value[infoHash]?.let { current ->
            _downloads.value = _downloads.value + (infoHash to current.copy(status = DownloadStatus.DOWNLOADING))
        }
    }

    override suspend fun cancelDownload(infoHash: String, deleteFiles: Boolean): Result<Unit> = runCatching {
        // TODO (Codex): sessionManager.remove(handle, deleteFiles)
        _downloads.value = _downloads.value - infoHash
    }

    override suspend fun fetchTorrentMetadata(magnetUri: String, timeoutMs: Long): Result<TorrentInfo> = runCatching {
        val infoHash = magnetUri.substringAfter("btih:").substringBefore("&").lowercase()
        // TODO (Codex): sessionManager.fetchMagnet(magnetUri, timeoutMs)
        TorrentInfo(
            infoHash = infoHash,
            magnetUri = magnetUri,
        )
    }

    override fun observeDownloads(): Flow<List<DownloadItem>> {
        return _downloads.map { it.values.toList() }
    }

    override fun observeDownload(infoHash: String): Flow<DownloadItem?> {
        return _downloads.map { it[infoHash] }
    }

    override suspend fun setFilePriorities(infoHash: String, filePriorities: Map<Int, Boolean>): Result<Unit> = runCatching {
        // TODO (Codex): handle.prioritizeFiles(priorities)
    }

    override suspend fun updateNetworkSettings(
        wifiOnly: Boolean,
        maxDownloadSpeedBytes: Long,
        maxUploadSpeedBytes: Long,
    ) {
        // TODO (Codex): update libtorrent SettingsPack
    }
}
