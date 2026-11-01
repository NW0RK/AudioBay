package com.audiobookbay.app.ui.screens.downloads

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audiobookbay.app.data.storage.DownloadFolderSettings
import com.audiobookbay.app.data.storage.DownloadNetworkSettings
import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.domain.repository.AudiobookRepository
import com.audiobookbay.app.domain.repository.TorrentDownloadManager
import com.audiobookbay.app.torrent.DownloadStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject

enum class DownloadFormError { REQUIRED, FOLDER, ACTION }

/** Files shown in the selective-download dialog. */
data class FileSelection(val infoHash: String, val title: String, val files: List<TorrentFile>)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val manager: TorrentDownloadManager,
    private val storage: DownloadStorage,
    private val folders: DownloadFolderSettings,
    private val network: DownloadNetworkSettings,
    private val repository: AudiobookRepository,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<DownloadItem>>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()
    private val _error = MutableStateFlow<DownloadFormError?>(null)
    val error = _error.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _fileSelection = MutableStateFlow<FileSelection?>(null)
    val fileSelection = _fileSelection.asStateFlow()
    val folder = folders.folder.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val wifiOnly = network.wifiOnly.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val author = saved.getStateFlow("author", "")
    val title = saved.getStateFlow("bookTitle", "")
    val magnet = saved.getStateFlow("magnet", "")
    val editingTree = saved.getStateFlow<String?>("editingTree", null)
    val editing = saved.getStateFlow<String?>("editing", null)
    fun setAuthor(value: String) { saved["author"] = value }
    fun setTitle(value: String) { saved["bookTitle"] = value }
    fun setMagnet(value: String) { saved["magnet"] = value }
    fun folderLabel(uri: String?) = folders.display(uri)

    init {
        viewModelScope.launch {
            manager.observeDownloads().catch { _uiState.value = UiState.Error(it.message.orEmpty()) }
                .collect { _uiState.value = UiState.Success(it) }
        }
        saved.get<String>("audiobookId")?.let { id -> viewModelScope.launch {
            when (val result = repository.getAudiobookDetail(id)) {
                is Resource.Success -> {
                    if (title.value.isBlank()) setTitle(result.data.title)
                    if (author.value.isBlank() && result.data.author.lowercase() !in setOf("unknown", "unknown author")) setAuthor(result.data.author)
                    if (magnet.value.isBlank()) setMagnet(result.data.torrentInfo?.magnetUri.orEmpty())
                }
                else -> Unit
            }
        } }
    }
    fun selectFolder(uri: String) = action(DownloadFormError.FOLDER) {
        folders.select(uri)
        if (editing.value != null) saved["editingTree"] = uri
    }
    fun edit(item: DownloadItem) {
        saved["editing"] = item.infoHash
        saved["editingTree"] = item.destinationTreeUri
        setAuthor(item.author.orEmpty()); setTitle(item.title)
    }
    fun cancelEdit() { saved["editing"] = null; saved["editingTree"] = null }

    private suspend fun metadata(): DownloadMetadata {
        val tree = saved.get<String>("editingTree") ?: folders.folder.first().orEmpty()
        if (author.value.isBlank() || title.value.isBlank() || tree.isBlank()) {
            _error.value = DownloadFormError.REQUIRED
            error("Author, book title and shared folder are required")
        }
        try { folders.validate(tree) } catch (e: CancellationException) { throw e } catch (e: Exception) { _error.value = DownloadFormError.FOLDER; throw e }
        return DownloadMetadata(author.value.trim(), title.value.trim(), tree)
    }
    fun startMagnet() = action {
        val metadata = metadata()
        require(magnet.value.trim().startsWith("magnet:?"))
        manager.startDownload(saved.get<String>("audiobookId") ?: "import:${UUID.randomUUID()}", magnet.value.trim(), metadata).getOrThrow()
    }
    fun saveAndResume() = action {
        val hash = checkNotNull(editing.value)
        manager.updateMetadata(hash, metadata()).getOrThrow()
        manager.resumeDownload(hash).getOrThrow()
        cancelEdit()
    }
    fun importTorrent(uri: String) = action {
        val metadata = metadata()
        val file = storage.copyTorrent(uri)
        try { manager.startDownloadFromFile("import:${UUID.randomUUID()}", file, metadata).getOrThrow() }
        finally { withContext(NonCancellable) { storage.deleteTemporary(file) } }
    }
    fun pause(hash: String) = action { manager.initialize(); manager.pauseDownload(hash).getOrThrow() }
    fun resume(item: DownloadItem) {
        if (item.author.isNullOrBlank() || item.destinationTreeUri.isNullOrBlank() || item.status == DownloadStatus.PUBLICATION_FAILED) edit(item)
        else action { manager.resumeDownload(item.infoHash).getOrThrow() }
    }
    fun cancel(hash: String) = action { manager.cancelDownload(hash, deleteFiles = false).getOrThrow() }
    fun showFiles(item: DownloadItem) = action {
        _fileSelection.value = FileSelection(item.infoHash, item.title, manager.listFiles(item.infoHash).getOrThrow())
    }
    fun dismissFiles() { _fileSelection.value = null }
    fun applyFiles(selected: Set<Int>) = action {
        val current = _fileSelection.value ?: return@action
        val audio = current.files.filter { it.isAudioFile }
        manager.setFilePriorities(current.infoHash, audio.associate { it.index to (it.index in selected) }).getOrThrow()
        _fileSelection.value = null
    }
    fun dismissError() { _error.value = null }
    private fun action(reason: DownloadFormError = DownloadFormError.ACTION, block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true; _error.value = null
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (_error.value == null) _error.value = reason }
            finally { _busy.value = false }
        }
    }
}
