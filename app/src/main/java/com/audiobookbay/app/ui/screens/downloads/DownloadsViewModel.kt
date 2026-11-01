package com.audiobookbay.app.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.domain.repository.TorrentDownloadManager
import com.audiobookbay.app.torrent.DownloadStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val manager: TorrentDownloadManager,
    private val storage: DownloadStorage,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<DownloadItem>>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()
    private val _hasError = MutableStateFlow(false)
    val hasError = _hasError.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    init {
        viewModelScope.launch {
            manager.observeDownloads().catch { _uiState.value = UiState.Error(it.message.orEmpty()) }
                .collect { _uiState.value = UiState.Success(it) }
        }
    }
    fun startMagnet(magnet: String) = action {
        require(magnet.trim().startsWith("magnet:?"))
        manager.startDownload("import:${UUID.randomUUID()}", magnet.trim(), storage.newDirectory()).getOrThrow()
    }
    fun importTorrent(uri: String) = action {
        val file = storage.copyTorrent(uri)
        try { manager.startDownloadFromFile("import:${UUID.randomUUID()}", file, storage.newDirectory()).getOrThrow() }
        finally { withContext(NonCancellable) { storage.deleteTemporary(file) } }
    }
    fun pause(hash: String) = action { manager.initialize(); manager.pauseDownload(hash).getOrThrow() }
    fun resume(hash: String) = action { manager.resumeDownload(hash).getOrThrow() }
    fun cancel(hash: String) = action { manager.initialize(); manager.cancelDownload(hash, deleteFiles = false).getOrThrow() }
    fun dismissError() { _hasError.value = false }
    private fun action(block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        _hasError.value = false
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _hasError.value = true }
            finally { _busy.value = false }
        }
    }
}
