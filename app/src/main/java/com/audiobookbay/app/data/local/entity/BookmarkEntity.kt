package com.audiobookbay.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.audiobookbay.app.domain.model.Bookmark

@Entity(
    tableName = "bookmarks",
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
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val audiobookId: String,
    val chapterIndex: Int,
    val positionMs: Long,
    val label: String,
    val note: String? = null,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
) {
    fun toDomain(): Bookmark = Bookmark(
        id = id,
        audiobookId = audiobookId,
        chapterIndex = chapterIndex,
        positionMs = positionMs,
        label = label,
        note = note,
        createdAtTimestamp = createdAtTimestamp,
    )
}
