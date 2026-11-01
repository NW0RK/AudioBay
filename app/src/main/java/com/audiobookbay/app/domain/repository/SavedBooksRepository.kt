package com.audiobookbay.app.domain.repository
import com.audiobookbay.app.domain.model.*
import kotlinx.coroutines.flow.Flow
interface SavedBooksRepository {
    fun observeSaved(): Flow<Resource<List<Audiobook>>>
    suspend fun setSaved(book: Audiobook, saved: Boolean): Resource<Unit>
}
