package com.audiobookbay.app

import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.domain.repository.TorrentDownloadManager
import com.audiobookbay.app.torrent.DownloadStorage
import com.audiobookbay.app.ui.screens.downloads.DownloadsViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val manager = mockk<TorrentDownloadManager>()
    private val storage = mockk<DownloadStorage>()
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        every { manager.observeDownloads() } returns flowOf(emptyList())
    }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun viewingQueueDoesNotStartNativeEngine() = runTest(dispatcher) {
        val vm = DownloadsViewModel(manager, storage)
        runCurrent()
        assertEquals(UiState.Success(emptyList<DownloadItem>()), vm.uiState.value)
        coVerify(exactly = 0) { manager.initialize() }
    }
    @Test fun invalidMagnetProducesRecoverableError() = runTest(dispatcher) {
        val vm = DownloadsViewModel(manager, storage)
        vm.startMagnet("not a magnet")
        runCurrent()
        assertTrue(vm.hasError.value)
        assertFalse(vm.busy.value)
        coVerify(exactly = 0) { manager.startDownload(any(), any(), any(), any()) }
        vm.dismissError()
        assertFalse(vm.hasError.value)
    }
    @Test fun failedResumeDoesNotClaimSuccess() = runTest(dispatcher) {
        coEvery { manager.resumeDownload("hash") } returns Result.failure(IllegalStateException("Unavailable"))
        val vm = DownloadsViewModel(manager, storage)
        vm.resume("hash")
        vm.resume("hash")
        runCurrent()
        assertTrue(vm.hasError.value)
        coVerify(exactly = 1) { manager.resumeDownload("hash") }
    }
}
