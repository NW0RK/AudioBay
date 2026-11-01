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
    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { repository.getSearchHistory() } returns flowOf(emptyList())
    }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `typing debounces and only submits final query`() = runTest(dispatcher) {
        coEvery { repository.searchAudiobooks("Alice", 1) } returns Resource.Success(emptyList())
        val vm = SearchViewModel(repository)
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
        val vm = SearchViewModel(repository)
        vm.setQuery("old")
        advanceTimeBy(300)
        runCurrent()
        vm.setQuery("new")
        advanceUntilIdle()
        assertEquals("new", (vm.uiState.value as UiState.Success).data.single().id)
    }

    @Test fun `clearing query returns to idle without request`() = runTest(dispatcher) {
        val vm = SearchViewModel(repository)
        vm.setQuery("Alice")
        vm.setQuery("")
        advanceUntilIdle()
        assertEquals(UiState.Idle, vm.uiState.value)
        coVerify(exactly = 0) { repository.searchAudiobooks(any(), any()) }
    }
}
