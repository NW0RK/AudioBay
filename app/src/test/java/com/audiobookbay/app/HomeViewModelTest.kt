package com.audiobookbay.app

import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.domain.repository.AudiobookRepository
import com.audiobookbay.app.ui.screens.home.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<AudiobookRepository>()
    private fun book(id: String) = Audiobook(id, "Book $id", "Author")
    private fun HomeViewModel.content() = (uiState.value as UiState.Success).data

    @Before fun setUp() { Dispatchers.setMain(dispatcher); coEvery { repository.getCategories() } returns Resource.Success(emptyList()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `initial request displays books`() = runTest(dispatcher) {
        coEvery { repository.getRecentAudiobooks(1) } returns Resource.Success(listOf(book("1")))
        val vm = HomeViewModel(repository)
        assertEquals(UiState.Loading, vm.uiState.value)
        runCurrent()
        assertEquals(listOf(book("1")), vm.content().books)
    }

    @Test fun `pagination deduplicates and stops on empty page`() = runTest(dispatcher) {
        coEvery { repository.getRecentAudiobooks(1) } returns Resource.Success(listOf(book("1")))
        coEvery { repository.getRecentAudiobooks(2) } returns Resource.Success(listOf(book("1"), book("2")))
        coEvery { repository.getRecentAudiobooks(3) } returns Resource.Success(emptyList())
        val vm = HomeViewModel(repository)
        runCurrent()
        vm.loadMore()
        vm.loadMore()
        runCurrent()
        assertEquals(listOf(book("1"), book("2")), vm.content().books)
        vm.loadMore()
        runCurrent()
        vm.loadMore()
        assertTrue(vm.content().endReached)
        coVerify(exactly = 1) { repository.getRecentAudiobooks(2) }
        coVerify(exactly = 1) { repository.getRecentAudiobooks(3) }
    }

    @Test fun `failed pagination retains books and retries same page`() = runTest(dispatcher) {
        coEvery { repository.getRecentAudiobooks(1) } returns Resource.Success(listOf(book("1")))
        coEvery { repository.getRecentAudiobooks(2) } returnsMany listOf(Resource.Error("offline"), Resource.Success(listOf(book("2"))))
        val vm = HomeViewModel(repository)
        runCurrent()
        vm.loadMore()
        runCurrent()
        assertTrue(vm.content().hasError)
        assertEquals(1, vm.content().books.size)
        vm.loadMore()
        runCurrent()
        coVerify(exactly = 1) { repository.getRecentAudiobooks(2) }
        vm.retry()
        runCurrent()
        assertEquals(2, vm.content().books.size)
    }

    @Test fun `refresh cancels pending page and replaces old list`() = runTest(dispatcher) {
        coEvery { repository.getRecentAudiobooks(1) } returnsMany listOf(Resource.Success(listOf(book("1"))), Resource.Success(listOf(book("3"))))
        coEvery { repository.getRecentAudiobooks(2) } coAnswers { delay(1000); Resource.Success(listOf(book("2"))) }
        val vm = HomeViewModel(repository)
        runCurrent()
        vm.loadMore()
        runCurrent()
        vm.refresh()
        advanceUntilIdle()
        assertEquals(listOf(book("3")), vm.content().books)
    }

    @Test fun `initial error can be retried`() = runTest(dispatcher) {
        coEvery { repository.getRecentAudiobooks(1) } returnsMany listOf(Resource.Error("offline"), Resource.Success(emptyList()))
        val vm = HomeViewModel(repository)
        runCurrent()
        assertTrue(vm.uiState.value is UiState.Error)
        vm.retry()
        runCurrent()
        assertTrue(vm.content().books.isEmpty())
        assertTrue(vm.content().endReached)
    }

    @Test fun `failed refresh retries first page`() = runTest(dispatcher) {
        coEvery { repository.getRecentAudiobooks(1) } returnsMany listOf(Resource.Success(listOf(book("1"))), Resource.Error("offline"), Resource.Success(listOf(book("3"))))
        val vm = HomeViewModel(repository)
        runCurrent()
        vm.refresh()
        runCurrent()
        vm.retry()
        runCurrent()
        assertEquals(listOf(book("3")), vm.content().books)
    }
}
