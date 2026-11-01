package com.audiobookbay.app.ui.screens.downloads

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.audiobookbay.app.data.storage.DownloadFolderSettings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.audiobookbay.app.ui.components.EditorialPanel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.audiobookbay.app.R
import com.audiobookbay.app.domain.model.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(onBrowse: () -> Unit = {}) {
    val vm: DownloadsViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val correcting by vm.correcting.collectAsStateWithLifecycle()
    val correctionAvailable by vm.correctionAvailability.collectAsStateWithLifecycle()
    val correctionFailed by vm.correctionFailed.collectAsStateWithLifecycle()
    val canUndoNames by vm.canUndoNames.collectAsStateWithLifecycle()
    val author by vm.author.collectAsStateWithLifecycle()
    val title by vm.title.collectAsStateWithLifecycle()
    val magnet by vm.magnet.collectAsStateWithLifecycle()
    val folder by vm.folder.collectAsStateWithLifecycle()
    val wifiOnly by vm.wifiOnly.collectAsStateWithLifecycle()
    val fileSelection by vm.fileSelection.collectAsStateWithLifecycle()
    val editing by vm.editing.collectAsStateWithLifecycle()
    val editingTree by vm.editingTree.collectAsStateWithLifecycle()
    val effectiveFolder = if (editing != null) editingTree ?: folder else folder
    val context = LocalContext.current
    val grid = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(editing) { if (editing != null) grid.animateScrollToItem(0) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.importTorrent(it.toString()) } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun requestNotifications() { if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
    var setupExpanded by rememberSaveable { mutableStateOf(vm.isCatalogDownload) }
    var editingMetadata by rememberSaveable { mutableStateOf(false) }
    var editingLink by rememberSaveable { mutableStateOf(false) }
    var showingFolder by rememberSaveable { mutableStateOf(false) }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { vm.selectFolder(it.toString()) }
    }
    LaunchedEffect(error) { if (error != null) setupExpanded = true }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.nav_downloads)) })
        LazyColumn(state = grid, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                DownloadSetupForm(
                    state = DownloadSetupState(
                        title = title, author = author, magnet = magnet,
                        folderLabel = vm.folderLabel(effectiveFolder), folderSelected = effectiveFolder != null,
                        catalogBook = vm.isCatalogDownload, editingDownload = editing != null,
                        expanded = setupExpanded, editingMetadata = editingMetadata, editingLink = editingLink,
                        showingFolder = showingFolder, busy = busy, wifiOnly = wifiOnly, error = error,
                        correctionAvailable = correctionAvailable, correcting = correcting,
                        correctionFailed = correctionFailed, canUndoNames = canUndoNames,
                    ),
                    onToggleExpanded = { setupExpanded = !setupExpanded },
                    onToggleMetadata = { editingMetadata = !editingMetadata },
                    onToggleLink = { editingLink = !editingLink },
                    onToggleFolder = { showingFolder = !showingFolder },
                    onAuthorChange = vm::setAuthor, onTitleChange = vm::setTitle, onMagnetChange = vm::setMagnet,
                    onChooseFolder = { folderPicker.launch(DownloadFolderSettings.initialUri) },
                    onStart = { requestNotifications(); vm.startMagnet() },
                    onOpenTorrent = { requestNotifications(); picker.launch(arrayOf("application/x-bittorrent", "application/octet-stream")) },
                    onSaveResume = { requestNotifications(); vm.saveAndResume() },
                    onCancelEdit = vm::cancelEdit, onDismissError = vm::dismissError,
                    onFixNames = vm::fixNames, onUndoNames = vm::undoNames,
                )
            }
            item { com.audiobookbay.app.ui.components.SectionHeading(stringResource(R.string.download_queue)) }
            when (val result = state) {
                UiState.Idle, UiState.Loading -> item { CircularProgressIndicator() }
                is UiState.Error -> item { com.audiobookbay.app.ui.components.MessageState(stringResource(R.string.download_queue_error), stringResource(R.string.queue_error_hint), stringResource(R.string.retry), vm::retryQueue, error = true) }
                is UiState.Success -> {
                    if (result.data.isEmpty()) item { com.audiobookbay.app.ui.components.MessageState(stringResource(R.string.no_downloads), stringResource(R.string.download_empty_hint), stringResource(R.string.browse_audiobooks), onBrowse) }
                    items(result.data, key = { it.id }) { download ->
                        EditorialPanel(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(download.title, style = MaterialTheme.typography.titleLarge)
                                download.author?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                com.audiobookbay.app.ui.components.DownloadStatusBadge(download.status)
                                val publishing = download.status in setOf(DownloadStatus.PUBLISHING, DownloadStatus.PUBLICATION_FAILED)
                                com.audiobookbay.app.ui.components.DownloadProgress(download)
                                if (publishing) Text(stringResource(R.string.publication_stats, (download.publicationProgress * 100).toInt()))
                                else Text(stringResource(R.string.download_stats, (download.progress * 100).toInt(), String.format(Locale.getDefault(), "%.1f", download.downloadSpeedBytesPerSec / 1024.0), download.peers))
                                if (download.status == DownloadStatus.DOWNLOADING) {
                                    formatRemainingTime(download.etaSeconds)?.let { remaining ->
                                        Text(stringResource(R.string.download_eta, remaining), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                if (download.status == DownloadStatus.PUBLICATION_FAILED) Text(stringResource(R.string.publication_retry_hint))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    when {
                                        download.status in setOf(DownloadStatus.PAUSED, DownloadStatus.ERROR, DownloadStatus.PUBLICATION_FAILED, DownloadStatus.WAITING_FOR_WIFI) || (download.status == DownloadStatus.COMPLETED && download.publishedFolderUri == null) ->
                                            TextButton(shape = MaterialTheme.shapes.small, onClick = { vm.resume(download) }, enabled = !busy) { Text(stringResource(R.string.resume)) }
                                        download.status in setOf(DownloadStatus.QUEUED, DownloadStatus.CHECKING, DownloadStatus.DOWNLOADING) ->
                                            TextButton(shape = MaterialTheme.shapes.small, onClick = { vm.pause(download.infoHash) }, enabled = !busy) { Text(stringResource(R.string.pause)) }
                                    }
                                    download.publishedFolderUri?.let { uri ->
                                        TextButton(shape = MaterialTheme.shapes.small, onClick = {
                                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), "vnd.android.document/directory").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                                                .onFailure { android.widget.Toast.makeText(context, R.string.open_folder_hint, android.widget.Toast.LENGTH_LONG).show() }
                                        }) { Text(stringResource(R.string.open_folder)) }
                                    }
                                    Box {
                                        var menu by remember { mutableStateOf(false) }
                                        IconButton(shape = MaterialTheme.shapes.small, onClick = { menu = true }) { Icon(androidx.compose.material.icons.Icons.Default.MoreVert, stringResource(R.string.more_actions)) }
                                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                            if (download.status in setOf(DownloadStatus.QUEUED, DownloadStatus.CHECKING, DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED, DownloadStatus.WAITING_FOR_WIFI, DownloadStatus.ERROR)) {
                                                DropdownMenuItem(text = { Text(stringResource(R.string.select_files)) }, enabled = !busy, onClick = { menu = false; vm.showFiles(download) })
                                            }
                                            DropdownMenuItem(text = { Text(stringResource(R.string.remove_queue_only)) }, enabled = !busy, onClick = { menu = false; vm.cancel(download.infoHash) })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    fileSelection?.let { selection ->
        val audio = remember(selection) { selection.files.filter { it.isAudioFile } }
        var checked by remember(selection) { mutableStateOf(audio.filter { it.isSelectedForDownload }.map { it.index }.toSet()) }
        AlertDialog(
            onDismissRequest = vm::dismissFiles,
            title = { Text(stringResource(R.string.select_files_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.select_files_hint), style = MaterialTheme.typography.bodySmall)
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(audio, key = { it.index }) { file ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    checked = if (file.index in checked) checked - file.index else checked + file.index
                                },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = file.index in checked,
                                    onCheckedChange = { selected ->
                                        checked = if (selected) checked + file.index else checked - file.index
                                    },
                                )
                                Text(file.path, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(shape = MaterialTheme.shapes.small, onClick = { vm.applyFiles(checked) }, enabled = checked.isNotEmpty() && !busy) {
                    Text(stringResource(R.string.apply))
                }
            },
            dismissButton = { TextButton(shape = MaterialTheme.shapes.small, onClick = vm::dismissFiles) { Text(stringResource(R.string.cancel_edit)) } },
        )
    }
}

internal fun statusLabel(status: DownloadStatus): Int = when (status) {
    DownloadStatus.QUEUED -> R.string.download_queued
    DownloadStatus.CHECKING -> R.string.download_checking
    DownloadStatus.DOWNLOADING -> R.string.download_downloading
    DownloadStatus.PAUSED -> R.string.download_paused
    DownloadStatus.WAITING_FOR_WIFI -> R.string.download_waiting_for_wifi
    DownloadStatus.PUBLISHING -> R.string.download_publishing
    DownloadStatus.PUBLICATION_FAILED -> R.string.download_publication_failed
    DownloadStatus.COMPLETED, DownloadStatus.SEEDING -> R.string.download_completed
    DownloadStatus.ERROR -> R.string.download_failed
    DownloadStatus.CANCELLED -> R.string.download_cancelled
}
