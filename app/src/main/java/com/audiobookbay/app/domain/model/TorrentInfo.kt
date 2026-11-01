package com.audiobookbay.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Torrent metadata associated with an audiobook.
 */
@Serializable
data class TorrentInfo(
    val infoHash: String,
    val magnetUri: String,
    val torrentFileUrl: String? = null,
    val totalSizeBytes: Long = 0L,
    val formattedSize: String = "",
    val seeders: Int = 0,
    val leechers: Int = 0,
    val files: List<TorrentFile> = emptyList(),
    val trackerUrls: List<String> = emptyList(),
)

@Serializable
data class TorrentFile(
    val index: Int,
    val path: String,
    val sizeBytes: Long,
    val isAudioFile: Boolean = true,
    val isSelectedForDownload: Boolean = true,
)
