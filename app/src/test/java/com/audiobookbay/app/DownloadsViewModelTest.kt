package com.audiobookbay.app

import androidx.lifecycle.SavedStateHandle
import com.audiobookbay.app.data.storage.DownloadFolderSettings
import com.audiobookbay.app.data.storage.DownloadNetworkSettings
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
    private val network = mockk<DownloadNetworkSettings>()
    private val repository = mockk<AudiobookRepository>()
    private val corrector = mockk<BookNameCorrector>()
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        every { corrector.available } returns false
        every { corrector.availability } answers { flowOf(corrector.available) }
        every { manager.observeDownloads() } returns flowOf(emptyList())
        every { folders.folder } returns flowOf("content://books")
        every { network.wifiOnly } returns flowOf(false)
        coEvery { folders.validate(any()) } returns Unit
    }
    @After fun tearDown() { Dispatchers.resetMain() }
    private fun vm(saved: SavedStateHandle = SavedStateHandle()) = DownloadsViewModel(manager, storage, folders, network, repository, saved, corrector)

    @Test fun correctionControlsReactToKeyAddedAndRemoved() = runTest(dispatcher) {
        val availability = kotlinx.coroutines.flow.MutableStateFlow(false)
        every { corrector.availability } returns availability
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.correctionAvailability.collect { }
        }
        runCurrent()
        assertFalse(vm.correctionAvailability.value)
        availability.value = true
        runCurrent()
        assertTrue(vm.correctionAvailability.value)
        availability.value = false
        runCurrent()
        assertFalse(vm.correctionAvailability.value)
    }

    @Test fun correctionUndoAndDownloadUseDisplayedNames() = runTest(dispatcher) {
        every { corrector.available } returns true
        coEvery { corrector.correct(any(), any()) } returns Resource.Success(CorrectedBookNames("Dune", "Frank Herbert"))
        coEvery { manager.startDownload(any(), any(), any(), any()) } returns Result.success(DownloadItem("hash", "id", "Dune", "hash", "staging"))
        val vm = vm()
        vm.setTitle("DUNE: Subtitle [MP3]"); vm.setAuthor("FRANK HERBERT"); vm.setMagnet("magnet:?xt=test")
        vm.fixNames(); runCurrent()
        assertEquals("Dune", vm.title.value); assertTrue(vm.canUndoNames.value)
        vm.undoNames()
        assertEquals("DUNE: Subtitle [MP3]", vm.title.value); assertEquals("FRANK HERBERT", vm.author.value)
        vm.fixNames(); runCurrent(); vm.startMagnet(); runCurrent()
        coVerify { manager.startDownload(any(), any(), DownloadMetadata("Frank Herbert", "Dune", "content://books"), any()) }
        coVerify(exactly = 2) { corrector.correct(any(), any()) }
    }

    @Test fun autoCorrectionAndUndoSurviveRecreationWithoutAnotherRequest() = runTest(dispatcher) {
        every { corrector.available } returns true
        coEvery { repository.getAudiobookDetail("catalog-book") } returns Resource.Success(AudiobookDetail("catalog-book", "DUNE [MP3]", "FRANK HERBERT"))
        coEvery { corrector.correct(any(), any()) } returns Resource.Success(CorrectedBookNames("Dune", "Frank Herbert"))
        val state = SavedStateHandle(mapOf("audiobookId" to "catalog-book"))
        val first = vm(state); runCurrent()
        assertEquals("Dune", first.title.value)
        val restored = vm(SavedStateHandle(state.keys().associateWith { state.get<Any?>(it) })); runCurrent()
        assertEquals("Dune", restored.title.value); assertTrue(restored.canUndoNames.value)
        restored.undoNames(); assertEquals("DUNE [MP3]", restored.title.value)
        coVerify(exactly = 1) { corrector.correct(any(), any()) }
    }

    @Test fun editingCancelsCorrectionAndDiscardsNonCooperativeResponse() = runTest(dispatcher) {
        every { corrector.available } returns true
        coEvery { corrector.correct(any(), any()) } coAnswers {
            withContext(NonCancellable) { delay(100); Resource.Success(CorrectedBookNames("Stale", "Stale")) }
        }
        val vm = vm(); vm.setTitle("Original"); vm.setAuthor("Author"); vm.fixNames(); runCurrent()
        assertTrue(vm.correcting.value)
        vm.startMagnet(); runCurrent()
        coVerify(exactly = 0) { manager.startDownload(any(), any(), any(), any()) }
        vm.setTitle("My title"); assertFalse(vm.correcting.value)
        advanceUntilIdle()
        assertEquals("My title", vm.title.value); assertEquals("Author", vm.author.value)
    }

    @Test fun failurePreservesNamesAndAllowsExplicitRetry() = runTest(dispatcher) {
        every { corrector.available } returns true
        coEvery { corrector.correct(any(), any()) } returnsMany listOf(Resource.Error("quota"), Resource.Success(CorrectedBookNames("Clean", null)))
        val vm = vm(); vm.setTitle("Original"); vm.setAuthor("Author"); vm.fixNames(); runCurrent()
        assertTrue(vm.correctionFailed.value); assertFalse(vm.correcting.value)
        assertEquals("Original", vm.title.value)
        vm.fixNames(); runCurrent()
        assertFalse(vm.correctionFailed.value); assertEquals("Clean", vm.title.value); assertEquals("Author", vm.author.value)
    }

    @Test fun missingMetadataCannotBeInventedByModel() = runTest(dispatcher) {
        every { corrector.available } returns true
        coEvery { corrector.correct(any(), any()) } returns Resource.Success(CorrectedBookNames("Clean", "Invented"))
        val vm = vm(); vm.setTitle("Original"); vm.fixNames(); runCurrent()
        assertEquals("Clean", vm.title.value); assertEquals("", vm.author.value)
        vm.startMagnet(); runCurrent(); assertEquals(DownloadFormError.REQUIRED, vm.error.value)
    }

    @Test fun switchingEditedDownloadInvalidatesPendingCorrection() = runTest(dispatcher) {
        every { corrector.available } returns true
        coEvery { corrector.correct(any(), any()) } coAnswers {
            withContext(NonCancellable) { delay(100); Resource.Success(CorrectedBookNames("Wrong", "Wrong")) }
        }
        val vm = vm(); vm.setTitle("Original"); vm.fixNames(); runCurrent()
        vm.edit(DownloadItem("hash", "id", "Other", "hash", "staging", author = "Other Author"))
        advanceUntilIdle()
        assertEquals("Other", vm.title.value); assertEquals("Other Author", vm.author.value)
    }

    @Test fun disabledCorrectionDoesNotCallApi() = runTest(dispatcher) {
        val vm = vm(); vm.setTitle("Original"); vm.fixNames(); runCurrent()
        assertFalse(vm.correctionAvailable); assertFalse(vm.correcting.value)
        coVerify(exactly = 0) { corrector.correct(any(), any()) }
    }

    @Test fun clearingAuthorWhileCatalogLoadsIsPreserved() = runTest(dispatcher) {
        every { corrector.available } returns true
        coEvery { repository.getAudiobookDetail("catalog-book") } coAnswers {
            delay(100); Resource.Success(AudiobookDetail("catalog-book", "Book", "Catalog Author"))
        }
        val vm = vm(SavedStateHandle(mapOf("audiobookId" to "catalog-book")))
        vm.setAuthor(""); advanceUntilIdle()
        assertEquals("", vm.author.value); assertEquals("Book", vm.title.value)
        coVerify(exactly = 0) { corrector.correct(any(), any()) }
    }

    @Test fun inFlightAttemptIsNotRepeatedAfterProcessRestoration() = runTest(dispatcher) {
        every { corrector.available } returns true
        coEvery { repository.getAudiobookDetail("catalog-book") } returns Resource.Success(AudiobookDetail("catalog-book", "Book", "Author"))
        val vm = vm(SavedStateHandle(mapOf("audiobookId" to "catalog-book", "bookTitle" to "Original", "author" to "My Author", "autoNamesAttempted" to true)))
        runCurrent()
        assertEquals("Original", vm.title.value); assertFalse(vm.correcting.value)
        coVerify(exactly = 0) { corrector.correct(any(), any()) }
    }
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
        assertNull(vm.error.value)
        assertEquals(setOf("hash"), vm.queueErrors.value)
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
        val vm = DownloadsViewModel(manager, storage, folders, network, repository, SavedStateHandle(mapOf("audiobookId" to "catalog-book")), corrector)
        vm.setAuthor("Corrected Author")
        advanceUntilIdle()
        assertEquals("Corrected Author", vm.author.value)
        assertEquals("Book Title", vm.title.value)
        assertEquals("magnet:?xt=test", vm.magnet.value)
    }
    @Test fun unknownCatalogAuthorStillRequiresUserInput() = runTest(dispatcher) {
        coEvery { repository.getAudiobookDetail("catalog-book") } returns Resource.Success(AudiobookDetail("catalog-book", "Book Title", "Unknown Author"))
        val vm = DownloadsViewModel(manager, storage, folders, network, repository, SavedStateHandle(mapOf("audiobookId" to "catalog-book")), corrector)
        runCurrent()
        assertEquals("", vm.author.value)
        assertEquals("Book Title", vm.title.value)
    }
    @Test fun queueFailureStaysOnItsItemAndRetryClearsIt() = runTest(dispatcher) {
        coEvery { manager.initialize() } returns Unit
        coEvery { manager.pauseDownload("hash") } returns Result.failure(IllegalStateException("offline"))
        val vm = vm()
        vm.pause("hash"); runCurrent()
        assertEquals(setOf("hash"), vm.queueErrors.value)
        assertNull(vm.error.value)
        assertTrue(vm.pendingItem.value.isEmpty())
        coEvery { manager.pauseDownload("hash") } returns Result.success(Unit)
        vm.pause("hash"); runCurrent()
        assertTrue(vm.queueErrors.value.isEmpty())
    }
}
