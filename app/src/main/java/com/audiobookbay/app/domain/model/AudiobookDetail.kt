package com.audiobookbay.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Detailed representation of an audiobook including synopsis, torrent metadata, and file listing.
 */
@Serializable
data class AudiobookDetail(
    val id: String,
    val title: String,
    val author: String,
    val narrator: String? = null,
    val description: String = "",
    val coverImageUrl: String? = null,
    val categories: List<String> = emptyList(),
    val language: String = "English",
    val audioFormat: String = "MP3",
    val bitrate: String? = null,
    val durationSeconds: Long? = null,
    val totalSizeBytes: Long = 0L,
    val formattedSize: String = "",
    val torrentInfo: TorrentInfo? = null,
    val chapters: List<Chapter> = emptyList(),
    val source: AudiobookSource = AudiobookSource.AUDIOBOOKBAY,
    val datePosted: String? = null,
)
