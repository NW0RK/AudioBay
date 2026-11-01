package com.audiobookbay.app

import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.domain.repository.AudiobookSearchRepository
import com.audiobookbay.app.ui.screens.search.SearchViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<AudiobookSearchRepository>()
    private val settings = mockk<com.audiobookbay.app.data.storage.SearchFormattingSettings>()
    private val corrector = mockk<com.audiobookbay.app.domain.repository.BookNameCorrector>()
    private val enabled = kotlinx.coroutines.flow.MutableStateFlow(false)
    private val available = kotlinx.coroutines.flow.MutableStateFlow(true)
    private fun vm() = SearchViewModel(repository, settings, corrector)
    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { settings.enabled } returns enabled
        every { corrector.availability } returns available
        every { repository.getSearchHistory() } returns flowOf(emptyList())
    }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `typing debounces and only submits final query`() = runTest(dispatcher) {
        coEvery { repository.searchAudiobooks("Alice", 1) } returns Resource.Success(emptyList())
        val vm = vm()
        vm.setQuery("Al")
        advanceTimeBy(200)
        vm.setQuery("Alice")
        advanceTimeBy(299)
        coVerify(exactly = 0) { repository.searchAudiobooks(any(), any()) }
        advanceTimeBy(1)
        runCurrent()
        coVerify(exactly = 1) { repository.searchAudiobooks("Alice", 1) }
        assertTrue(vm.uiState.value is UiState.Success)
    }

    @Test fun `changing query cancels stale response`() = runTest(dispatcher) {
        coEvery { repository.searchAudiobooks("old", 1) } coAnswers { delay(1000); Resource.Success(listOf(Audiobook("old", "Old", "Author"))) }
        coEvery { repository.searchAudiobooks("new", 1) } returns Resource.Success(listOf(Audiobook("new", "New", "Author")))
        val vm = vm()
        vm.setQuery("old")
        advanceTimeBy(300)
        runCurrent()
        vm.setQuery("new")
        advanceUntilIdle()
        assertEquals("new", (vm.uiState.value as UiState.Success).data.single().id)
    }

    @Test fun `clearing query returns to idle without request`() = runTest(dispatcher) {
        val vm = vm()
        vm.setQuery("Alice")
        vm.setQuery("")
        advanceUntilIdle()
        assertEquals(UiState.Idle, vm.uiState.value)
        coVerify(exactly = 0) { repository.searchAudiobooks(any(), any()) }
    }
    @Test fun `submit normalizes whitespace and skips debounce`() = runTest(dispatcher) {
        coEvery { repository.searchAudiobooks("Alice Wonderland", 1) } returns Resource.Success(emptyList())
        val vm = vm()
        vm.setQuery("  Alice   Wonderland  ")
        vm.submit()
        runCurrent()
        coVerify(exactly = 1) { repository.searchAudiobooks("Alice Wonderland", 1) }
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.searchAudiobooks(any(), any()) }
    }

    @Test fun `same query does not restart pending search`() = runTest(dispatcher) {
        coEvery { repository.searchAudiobooks("Alice", 1) } returns Resource.Success(emptyList())
        val vm = vm()
        vm.setQuery("Alice")
        advanceTimeBy(200)
        vm.setQuery("Alice")
        advanceTimeBy(100)
        runCurrent()
        coVerify(exactly = 1) { repository.searchAudiobooks("Alice", 1) }
    }
    @Test fun `reset cancels search and clears results and pagination`() = runTest(dispatcher) {
        coEvery { repository.searchAudiobooks("old", 1) } coAnswers { delay(1000); Resource.Success(listOf(Audiobook("old", "Old", "Author"))) }
        val vm = vm()
        vm.setQuery("old")
        advanceTimeBy(300); runCurrent()
        vm.reset(); advanceUntilIdle()
        assertEquals("", vm.query.value)
        assertEquals(UiState.Idle, vm.uiState.value)
        assertFalse(vm.isLoadingMore.value)
        assertFalse(vm.pageError.value)
        assertFalse(vm.endReached.value)
        vm.loadMore(); runCurrent()
        coVerify(exactly = 1) { repository.searchAudiobooks(any(), any()) }
        coVerify(exactly = 0) { repository.clearSearchHistory() }
    }

    @Test fun disabledFormattingMakesNoAiRequests() = runTest(dispatcher) {
        coEvery { repository.searchAudiobooks(any(), 1) } returns Resource.Success(listOf(Audiobook("id", "RAW TITLE", "AUTHOR")))
        val vm = vm(); vm.setQuery("book"); advanceUntilIdle()
        coVerify(exactly = 0) { corrector.formatSearchResults(any()) }
        assertEquals("RAW TITLE", (vm.uiState.value as UiState.Success).data.single().title)
    }

    @Test fun enabledFormattingShowsOriginalsImmediatelyAndPreservesIdentity() = runTest(dispatcher) {
        enabled.value = true
        val book = Audiobook("id", "RAW TITLE", "AUTHOR", coverImageUrl = "cover", detailUrl = "details")
        coEvery { repository.searchAudiobooks(any(), 1) } returns Resource.Success(listOf(book))
        coEvery { corrector.formatSearchResults(any()) } coAnswers {
            delay(1000); Resource.Success(listOf(com.audiobookbay.app.domain.repository.CorrectedBookNames("Tidy Title", null)))
        }
        val vm = vm(); vm.setQuery("book"); advanceTimeBy(300); runCurrent()
        assertEquals(book, (vm.uiState.value as UiState.Success).data.single())
        assertTrue(vm.formatting.value)
        advanceUntilIdle()
        assertEquals(book.copy(title = "Tidy Title"), (vm.uiState.value as UiState.Success).data.single())
        enabled.value = false; runCurrent()
        assertEquals(book, (vm.uiState.value as UiState.Success).data.single())
    }

    @Test fun formattingFailureDoesNotFailSearch() = runTest(dispatcher) {
        enabled.value = true
        val book = Audiobook("id", "RAW TITLE", "AUTHOR")
        coEvery { repository.searchAudiobooks(any(), 1) } returns Resource.Success(listOf(book))
        coEvery { corrector.formatSearchResults(any()) } returns Resource.Error("quota")
        val vm = vm(); vm.setQuery("book"); advanceUntilIdle()
        assertEquals(listOf(book), (vm.uiState.value as UiState.Success).data)
        assertTrue(vm.formattingFailed.value)
        assertFalse(vm.formatting.value)
    }

    @Test fun queryChangesAndDisablingCancelStaleFormatting() = runTest(dispatcher) {
        enabled.value = true
        coEvery { repository.searchAudiobooks(any(), 1) } answers { Resource.Success(listOf(Audiobook(firstArg(), firstArg(), "AUTHOR"))) }
        coEvery { corrector.formatSearchResults(any()) } coAnswers {
            delay(1000); Resource.Success(listOf(com.audiobookbay.app.domain.repository.CorrectedBookNames("Clean", "Author")))
        }
        val vm = vm(); vm.setQuery("old"); advanceTimeBy(300); runCurrent()
        vm.setQuery("new"); advanceTimeBy(300); runCurrent()
        enabled.value = false; advanceUntilIdle()
        assertEquals("new", (vm.uiState.value as UiState.Success).data.single().title)
        assertFalse(vm.formatting.value)
        assertFalse(vm.formattingFailed.value)
    }

    @Test fun paginationFormatsOnlyNewBooks() = runTest(dispatcher) {
        enabled.value = true
        val first = Audiobook("1", "FIRST", "AUTHOR")
        val second = Audiobook("2", "SECOND", "AUTHOR")
        coEvery { repository.searchAudiobooks(any(), 1) } returns Resource.Success(listOf(first))
        coEvery { repository.searchAudiobooks(any(), 2) } returns Resource.Success(listOf(first, second))
        coEvery { corrector.formatSearchResults(any()) } coAnswers {
            val books = firstArg<List<Audiobook>>()
            Resource.Success(books.map { com.audiobookbay.app.domain.repository.CorrectedBookNames(it.title.lowercase(), "Author") })
        }
        val vm = vm(); vm.setQuery("book"); advanceUntilIdle(); vm.loadMore(); advanceUntilIdle()
        assertEquals(listOf("first", "second"), (vm.uiState.value as UiState.Success).data.map { it.title })
        coVerify(exactly = 1) { corrector.formatSearchResults(listOf(first)) }
        coVerify(exactly = 1) { corrector.formatSearchResults(listOf(second)) }
    }
    @Test fun enabledWithoutApiKeyKeepsOriginalsWithoutAiRequests() = runTest(dispatcher) {
        enabled.value = true; available.value = false
        val book = Audiobook("id", "RAW TITLE", "AUTHOR")
        coEvery { repository.searchAudiobooks(any(), 1) } returns Resource.Success(listOf(book))
        val vm = vm(); vm.setQuery("book"); advanceUntilIdle()
        assertEquals(listOf(book), (vm.uiState.value as UiState.Success).data)
        coVerify(exactly = 0) { corrector.formatSearchResults(any()) }
    }
}
