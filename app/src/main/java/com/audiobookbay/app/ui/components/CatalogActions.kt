package com.audiobookbay.app.ui.components

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.domain.repository.TorrentDownloadManager
import com.audiobookbay.app.ui.screens.saved.SavedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class CatalogDownloadsViewModel @Inject constructor(manager: TorrentDownloadManager) : ViewModel() {
    val downloads = manager.observeDownloads().catch { emit(emptyList()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
class CatalogActions(
    private val saved: UiState<List<Audiobook>>,
    private val failed: Audiobook?,
    private val downloads: List<DownloadItem>,
    private val change: (Audiobook, Boolean) -> Unit,
    val retryLoad: () -> Unit,
) {
    val loadFailed get() = saved is UiState.Error
    fun state(book: Audiobook): BookCardState = BookCardState(
        saved = (saved as? UiState.Success)?.data?.any { it.id == book.id && it.source == book.source } == true,
        saveEnabled = saved is UiState.Success,
        saveFailed = failed?.let { it.id == book.id && it.source == book.source } == true,
        download = downloads.firstOrNull { (book.infoHash != null && it.infoHash.equals(book.infoHash, true)) || it.audiobookId == book.id },
    )
    fun toggle(book: Audiobook) = change(book, !state(book).saved)
}
@Composable
fun rememberCatalogActions(savedVm: SavedViewModel = hiltViewModel(), downloadsVm: CatalogDownloadsViewModel = hiltViewModel()): CatalogActions {
    val saved by savedVm.state.collectAsStateWithLifecycle()
    val failed by savedVm.failedBook.collectAsStateWithLifecycle()
    val downloads by downloadsVm.downloads.collectAsStateWithLifecycle()
    return remember(saved, failed, downloads) { CatalogActions(saved, failed, downloads, savedVm::change, savedVm::retryLoad) }
}
