package com.audiobookbay.app.data.local.entity
import androidx.room.Entity
@Entity(tableName = "saved_books", primaryKeys = ["source", "bookId"])
data class SavedBookEntity(val source: String, val bookId: String, val payload: String, val savedAt: Long)
