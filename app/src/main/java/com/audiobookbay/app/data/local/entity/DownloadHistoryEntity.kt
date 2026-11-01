package com.audiobookbay.app.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
@Serializable
@Entity(tableName = "download_history")
data class DownloadHistoryEntity(@PrimaryKey val infoHash: String, val audiobookId: String, val title: String, val author: String, val completedAt: Long)
