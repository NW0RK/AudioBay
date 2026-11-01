package com.audiobookbay.app.data.repository
import com.audiobookbay.app.data.local.dao.SavedBookDao
import com.audiobookbay.app.data.local.entity.SavedBookEntity
import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.domain.repository.SavedBooksRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import javax.inject.Inject
class SavedBooksRepositoryImpl @Inject constructor(
    private val dao: SavedBookDao, private val json: Json,
    @com.audiobookbay.app.di.IoDispatcher private val io: CoroutineDispatcher,
) : SavedBooksRepository {
    override fun observeSaved(): Flow<Resource<List<Audiobook>>> = dao.observeAll()
        .map<List<SavedBookEntity>, Resource<List<Audiobook>>> { rows ->
            Resource.Success(rows.map { json.decodeFromString<Audiobook>(it.payload) })
        }.catch { emit(Resource.Error("Unable to read saved books", it)) }.flowOn(io)
    override suspend fun setSaved(book: Audiobook, saved: Boolean): Resource<Unit> = withContext(io) {
        try {
            if (saved) dao.insert(SavedBookEntity(book.source.name, book.id, json.encodeToString(book), System.currentTimeMillis()))
            else dao.remove(book.source.name, book.id)
            Resource.Success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Resource.Error("Unable to update saved books", e) }
    }
}
