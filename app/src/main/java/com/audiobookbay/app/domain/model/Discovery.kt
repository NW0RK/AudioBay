package com.audiobookbay.app.domain.model
import kotlinx.serialization.Serializable
@Serializable data class DownloadedBook(val infoHash: String, val audiobookId: String, val title: String, val author: String, val completedAt: Long)
@Serializable data class DiscoveryPick(val book: Audiobook, val reason: String)
