package com.audiobookbay.app

import androidx.lifecycle.SavedStateHandle
import com.audiobookbay.app.data.storage.DownloadFolderSettings
import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.domain.repository.*
import com.audiobookbay.app.torrent.DownloadStorage
import com.audiobookbay.app.ui.screens.downloads.*
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
    private val folders = mockk<DownloadFolderSettings>()
    private val repository = mockk<AudiobookRepository>()
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        every { manager.observeDownloads() } returns flowOf(emptyList())
        every { folders.folder } returns flowOf("content://books")
        coEvery { folders.validate(any()) } returns Unit
    }
    @After fun tearDown() { Dispatchers.resetMain() }
    private fun vm() = DownloadsViewModel(manager, storage, folders, repository, SavedStateHandle())
    @Test fun viewingQueueDoesNotStartNativeEngine() = runTest(dispatcher) {
        val vm = vm(); runCurrent()
        assertEquals(UiState.Success(emptyList<DownloadItem>()), vm.uiState.value)
        coVerify(exactly = 0) { manager.initialize() }
    }
    @Test fun authorAndTitleAreRequiredBeforeDownload() = runTest(dispatcher) {
        val vm = vm(); vm.setMagnet("magnet:?xt=test"); vm.startMagnet(); runCurrent()
        assertEquals(DownloadFormError.REQUIRED, vm.error.value)
        coVerify(exactly = 0) { manager.startDownload(any(), any(), any(), any()) }
    }
    @Test fun failedResumeDoesNotClaimSuccess() = runTest(dispatcher) {
        coEvery { manager.resumeDownload("hash") } returns Result.failure(IllegalStateException("Unavailable"))
        val vm = vm()
        val item = DownloadItem("hash", "id", "Book", "hash", "staging", author = "Author", destinationTreeUri = "content://books")
        vm.resume(item); vm.resume(item); runCurrent()
        assertEquals(DownloadFormError.ACTION, vm.error.value)
        coVerify(exactly = 1) { manager.resumeDownload("hash") }
    }
    @Test fun legacyDownloadPromptsForMissingMetadata() = runTest(dispatcher) {
        val vm = vm(); vm.resume(DownloadItem("hash", "id", "Book", "hash", "staging")); runCurrent()
        assertEquals("hash", vm.editing.value)
        assertEquals("Book", vm.title.value)
        assertEquals("", vm.author.value)
        coVerify(exactly = 0) { manager.resumeDownload(any()) }
    }
    @Test fun lostFolderGrantBlocksNewDownload() = runTest(dispatcher) {
        coEvery { folders.validate(any()) } throws SecurityException("Revoked")
        val vm = vm(); vm.setAuthor("Author"); vm.setTitle("Book"); vm.setMagnet("magnet:?xt=test"); vm.startMagnet(); runCurrent()
        assertEquals(DownloadFormError.FOLDER, vm.error.value)
        coVerify(exactly = 0) { manager.startDownload(any(), any(), any(), any()) }
    }
    @Test fun catalogPrefillPreservesUserCorrectionsWhileLoading() = runTest(dispatcher) {
        coEvery { repository.getAudiobookDetail("catalog-book") } coAnswers {
            delay(100)
            Resource.Success(AudiobookDetail("catalog-book", "Book Title", "Catalog Author",
                torrentInfo = TorrentInfo("hash", "magnet:?xt=test")))
        }
        val vm = DownloadsViewModel(manager, storage, folders, repository, SavedStateHandle(mapOf("audiobookId" to "catalog-book")))
        vm.setAuthor("Corrected Author")
        advanceUntilIdle()
        assertEquals("Corrected Author", vm.author.value)
        assertEquals("Book Title", vm.title.value)
        assertEquals("magnet:?xt=test", vm.magnet.value)
    }
    @Test fun unknownCatalogAuthorStillRequiresUserInput() = runTest(dispatcher) {
        coEvery { repository.getAudiobookDetail("catalog-book") } returns Resource.Success(AudiobookDetail("catalog-book", "Book Title", "Unknown Author"))
        val vm = DownloadsViewModel(manager, storage, folders, repository, SavedStateHandle(mapOf("audiobookId" to "catalog-book")))
        runCurrent()
        assertEquals("", vm.author.value)
        assertEquals("Book Title", vm.title.value)
    }
}
