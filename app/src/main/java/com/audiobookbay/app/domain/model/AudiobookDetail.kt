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
    val language: String = "",
    val audioFormat: String = "",
    val bitrate: String? = null,
    val durationSeconds: Long? = null,
    val totalSizeBytes: Long = 0L,
    val formattedSize: String = "",
    val torrentInfo: TorrentInfo? = null,
    val chapters: List<Chapter> = emptyList(),
    val source: AudiobookSource = AudiobookSource.AUDIOBOOKBAY,
    val uploader: String? = null,
    val datePosted: String? = null,
    val comments: List<AudiobookComment> = emptyList(),
)

@Serializable
data class AudiobookComment(
    val id: String,
    val author: String,
    val date: String = "",
    val text: String,
)

fun AudiobookDetail.toSummary() = Audiobook(
    id = id, title = title, author = author, narrator = narrator,
    coverImageUrl = coverImageUrl, category = categories.firstOrNull(),
    formattedSize = formattedSize.takeIf { it.isNotBlank() }, source = source,
    language = language.takeIf { it.isNotBlank() }, datePosted = datePosted,
    uploader = uploader, audioFormat = audioFormat.takeIf { it.isNotBlank() }, bitrate = bitrate,
)
