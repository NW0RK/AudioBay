package com.audiobookbay.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.audiobookbay.app.domain.model.Chapter

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = AudiobookEntity::class,
            parentColumns = ["id"],
            childColumns = ["audiobookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["audiobookId"])],
)
data class ChapterEntity(
    @PrimaryKey val id: String,
    val audiobookId: String,
    val title: String,
    val trackNumber: Int,
    val startOffsetMs: Long = 0L,
    val durationMs: Long = 0L,
    val filePath: String? = null,
    val fileSizeBytes: Long = 0L,
    val mediaType: String = "audio/mpeg",
) {
    fun toDomain(): Chapter = Chapter(
        id = id,
        audiobookId = audiobookId,
        title = title,
        trackNumber = trackNumber,
        startOffsetMs = startOffsetMs,
        durationMs = durationMs,
        filePath = filePath,
        fileSizeBytes = fileSizeBytes,
        mediaType = mediaType,
    )
}
