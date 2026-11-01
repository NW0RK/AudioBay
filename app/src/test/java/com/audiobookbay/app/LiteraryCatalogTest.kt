package com.audiobookbay.app

import androidx.lifecycle.SavedStateHandle
import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.domain.repository.AudiobookRepository
import com.audiobookbay.app.ui.screens.categories.CategoriesViewModel
import com.audiobookbay.app.ui.screens.saved.*
import com.audiobookbay.app.ui.components.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiteraryCatalogTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<AudiobookRepository>()
    private fun book(id: String, title: String = id, author: String = "Author") = Audiobook(id, title, author)
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { repository.getCategories() } returns Resource.Success(listOf(Category("fantasy", "Fantasy")))
    }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun sortingPreservesRecentOrderAndHandlesCaseAndUnicode() {
        val books = listOf(book("1", "Zulu", "zoe"), book("2", "alpha", "Ada"), book("3", "Été", "Ada"))
        assertEquals(listOf("1", "2", "3"), sortSavedBooks(books, SavedSort.Recent).map { it.id })
        assertEquals(listOf("2", "1", "3"), sortSavedBooks(books, SavedSort.Title).map { it.id })
        assertEquals(listOf("2", "3", "1"), sortSavedBooks(books, SavedSort.Author).map { it.id })
        assertTrue(sortSavedBooks(emptyList(), SavedSort.Author).isEmpty())
    }
    @Test fun allStatusesHaveMeaningfulTones() {
        assertEquals(setOf(DownloadStatus.COMPLETED, DownloadStatus.SEEDING), DownloadStatus.entries.filter { downloadTone(it) == DownloadTone.Success }.toSet())
        assertEquals(DownloadTone.Warning, downloadTone(DownloadStatus.WAITING_FOR_WIFI))
        assertEquals(DownloadTone.Warning, downloadTone(DownloadStatus.PAUSED))
        assertEquals(DownloadTone.Error, downloadTone(DownloadStatus.PUBLICATION_FAILED))
        assertEquals(DownloadTone.Error, downloadTone(DownloadStatus.ERROR))
        assertEquals(DownloadTone.Neutral, downloadTone(DownloadStatus.PUBLISHING))
        assertEquals(DownloadTone.Neutral, downloadTone(DownloadStatus.CANCELLED))
    }
    @Test fun categoryPaginationDeduplicatesAndRetriesFailedPage() = runTest(dispatcher) {
        coEvery { repository.getAudiobooksByCategory("fantasy", 1) } returns Resource.Success(listOf(book("1")))
        coEvery { repository.getAudiobooksByCategory("fantasy", 2) } returnsMany listOf(Resource.Error("offline"), Resource.Success(listOf(book("1"), book("2"))))
        coEvery { repository.getAudiobooksByCategory("fantasy", 3) } returns Resource.Success(emptyList())
        val vm = CategoriesViewModel(repository, SavedStateHandle(mapOf("slug" to "fantasy")))
        runCurrent()
        vm.loadMore(); vm.loadMore(); runCurrent()
        assertTrue(vm.state.value.failed)
        assertEquals(listOf("1"), vm.state.value.books.map { it.id })
        vm.retry(); runCurrent()
        assertEquals(listOf("1", "2"), vm.state.value.books.map { it.id })
        vm.loadMore(); runCurrent(); vm.loadMore()
        assertTrue(vm.state.value.endReached)
        coVerify(exactly = 2) { repository.getAudiobooksByCategory("fantasy", 2) }
        coVerify(exactly = 1) { repository.getAudiobooksByCategory("fantasy", 3) }
    }
    @Test fun changingCategoryCancelsOldResultAndClearingSelectionSurvivesRecreation() = runTest(dispatcher) {
        coEvery { repository.getAudiobooksByCategory("fantasy", 1) } coAnswers { delay(1000); Resource.Success(listOf(book("old"))) }
        coEvery { repository.getAudiobooksByCategory("history", 1) } returns Resource.Success(listOf(book("new")))
        val saved = SavedStateHandle(mapOf("slug" to "fantasy"))
        val vm = CategoriesViewModel(repository, saved)
        runCurrent(); vm.select("history"); advanceUntilIdle()
        assertEquals("new", vm.state.value.books.single().id)
        vm.select(null); runCurrent()
        assertNull(CategoriesViewModel(repository, saved).state.value.slug)
    }
    @Test fun categoryNetworkExceptionsBecomeRetryableState() = runTest(dispatcher) {
        coEvery { repository.getAudiobooksByCategory("fantasy", 1) } throws java.io.IOException("offline")
        val vm = CategoriesViewModel(repository, SavedStateHandle(mapOf("slug" to "fantasy")))
        runCurrent()
        assertTrue(vm.state.value.failed)
        assertFalse(vm.state.value.loading)
    }
}
