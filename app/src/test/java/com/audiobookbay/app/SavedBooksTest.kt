package com.audiobookbay.app
import androidx.lifecycle.viewModelScope
import com.audiobookbay.app.data.local.dao.SavedBookDao
import com.audiobookbay.app.data.local.entity.SavedBookEntity
import com.audiobookbay.app.data.repository.SavedBooksRepositoryImpl
import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.ui.screens.saved.SavedViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
@OptIn(ExperimentalCoroutinesApi::class)
class SavedBooksTest {
    private class Dao : SavedBookDao {
        val rows = MutableStateFlow<List<SavedBookEntity>>(emptyList())
        var fail = false
        override fun observeAll() = rows
        override suspend fun insert(book: SavedBookEntity) {
            check(!fail)
            if (rows.value.none { it.source == book.source && it.bookId == book.bookId }) rows.value = (rows.value + book).sortedByDescending { it.savedAt }
        }
        override suspend fun remove(source: String, id: String) {
            check(!fail)
            rows.value = rows.value.filterNot { it.source == source && it.bookId == id }
        }
    }
    @Test fun `saved snapshots deduplicate by source and retain offline metadata`() = runTest {
        val dao = Dao()
        val repo = SavedBooksRepositoryImpl(dao, Json, StandardTestDispatcher(testScheduler))
        val book = Audiobook("same", "Book", "Author", language = "French")
        repo.setSaved(book, true)
        val timestamp = dao.rows.value.single().savedAt
        repo.setSaved(book, true)
        assertEquals(timestamp, dao.rows.value.single().savedAt)
        repo.setSaved(book.copy(source = AudiobookSource.LIBRIVOX), true)
        assertEquals(2, (repo.observeSaved().first() as Resource.Success).data.size)
        repo.setSaved(book, false)
        assertEquals(AudiobookSource.LIBRIVOX, (repo.observeSaved().first() as Resource.Success).data.single().source)
        assertEquals("French", (repo.observeSaved().first() as Resource.Success).data.single().language)
        dao.fail = true
        assertTrue(repo.setSaved(book, true) is Resource.Error)
    }
    @Test fun `viewmodels synchronize only successful changes and retry failures`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dao = Dao()
            val repo = SavedBooksRepositoryImpl(dao, Json, dispatcher)
            val first = SavedViewModel(repo)
            val second = SavedViewModel(repo)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { first.state.collect() }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { second.state.collect() }
            runCurrent()
            val book = Audiobook("book", "Title", "Author")
            first.change(book, true); runCurrent()
            assertEquals(listOf(book), (second.state.value as UiState.Success).data)
            dao.fail = true
            second.change(book, false); runCurrent()
            assertEquals(book, second.failedBook.value)
            assertEquals(listOf(book), (first.state.value as UiState.Success).data)
            dao.fail = false
            second.retryChange(); runCurrent()
            assertTrue((first.state.value as UiState.Success).data.isEmpty())
            assertNull(second.failedBook.value)
            first.viewModelScope.cancel()
            second.viewModelScope.cancel()
            runCurrent()
        } finally { Dispatchers.resetMain() }
    }
}
