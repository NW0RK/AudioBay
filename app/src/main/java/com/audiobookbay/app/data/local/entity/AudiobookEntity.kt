package com.audiobookbay.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.audiobookbay.app.domain.model.Audiobook
import com.audiobookbay.app.domain.model.AudiobookSource

@Entity(tableName = "audiobooks")
data class AudiobookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val narrator: String?,
    val coverImageUrl: String?,
    val category: String?,
    val durationSeconds: Long?,
    val formattedSize: String?,
    val detailUrl: String?,
    val infoHash: String?,
    val source: String = AudiobookSource.AUDIOBOOKBAY.name,
    val description: String? = null,
    val isDownloaded: Boolean = false,
    val localDirectoryPath: String? = null,
    val addedAtTimestamp: Long = System.currentTimeMillis(),
) {
    fun toDomain(): Audiobook {
        return Audiobook(
            id = id,
            title = title,
            author = author,
            narrator = narrator,
            coverImageUrl = coverImageUrl,
            category = category,
            durationSeconds = durationSeconds,
            formattedSize = formattedSize,
            detailUrl = detailUrl,
            infoHash = infoHash,
            source = runCatching { AudiobookSource.valueOf(source) }.getOrDefault(AudiobookSource.AUDIOBOOKBAY),
        )
    }
}
