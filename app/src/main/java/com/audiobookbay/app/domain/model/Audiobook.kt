package com.audiobookbay.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Summary representation of an audiobook in listing / search results.
 */
@Serializable
data class Audiobook(
    val id: String,
    val title: String,
    val author: String,
    val narrator: String? = null,
    val coverImageUrl: String? = null,
    val category: String? = null,
    val durationSeconds: Long? = null,
    val formattedSize: String? = null,
    val language: String? = null,
    val datePosted: String? = null,
    val uploader: String? = null,
    val audioFormat: String? = null,
    val bitrate: String? = null,
    val detailUrl: String? = null,
    val infoHash: String? = null,
    val source: AudiobookSource = AudiobookSource.AUDIOBOOKBAY,
)

enum class AudiobookSource {
    AUDIOBOOKBAY,
    LIBRIVOX,
    INTERNET_ARCHIVE,
    LOCAL_DEVICE,
}
