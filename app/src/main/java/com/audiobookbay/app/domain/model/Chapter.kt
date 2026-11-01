package com.audiobookbay.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents an individual chapter or audio file within an audiobook.
 */
@Serializable
data class Chapter(
    val id: String,
    val audiobookId: String,
    val title: String,
    val trackNumber: Int,
    val startOffsetMs: Long = 0L,
    val durationMs: Long = 0L,
    val filePath: String? = null,
    val fileSizeBytes: Long = 0L,
    val mediaType: String = "audio/mpeg",
)
