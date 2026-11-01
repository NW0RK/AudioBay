package com.audiobookbay.app.domain.model

import kotlinx.serialization.Serializable

/**
 * User-created bookmark in an audiobook.
 */
@Serializable
data class Bookmark(
    val id: String,
    val audiobookId: String,
    val chapterIndex: Int,
    val positionMs: Long,
    val label: String,
    val note: String? = null,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
)
