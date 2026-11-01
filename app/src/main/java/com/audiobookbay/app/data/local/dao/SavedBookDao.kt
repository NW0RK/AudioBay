package com.audiobookbay.app.data.local.dao
import androidx.room.*
import com.audiobookbay.app.data.local.entity.SavedBookEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface SavedBookDao {
    @Query("SELECT * FROM saved_books ORDER BY savedAt DESC, source, bookId")
    fun observeAll(): Flow<List<SavedBookEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(book: SavedBookEntity)
    @Query("DELETE FROM saved_books WHERE source = :source AND bookId = :id")
    suspend fun remove(source: String, id: String)
}
