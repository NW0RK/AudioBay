package com.audiobookbay.app.ui.screens.downloads

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.audiobookbay.app.R
import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.ui.components.DownloadFolderPicker
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen() {
    val vm: DownloadsViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val author by vm.author.collectAsStateWithLifecycle()
    val title by vm.title.collectAsStateWithLifecycle()
    val magnet by vm.magnet.collectAsStateWithLifecycle()
    val folder by vm.folder.collectAsStateWithLifecycle()
    val editing by vm.editing.collectAsStateWithLifecycle()
    val editingTree by vm.editingTree.collectAsStateWithLifecycle()
    val effectiveFolder = if (editing != null) editingTree ?: folder else folder
    val context = LocalContext.current
    val grid = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(editing) { if (editing != null) grid.animateScrollToItem(0) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.importTorrent(it.toString()) } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun requestNotifications() { if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.nav_downloads)) })
        LazyColumn(state = grid, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                DownloadFolderPicker(vm.folderLabel(effectiveFolder), effectiveFolder != null, !busy, vm::selectFolder)
                Text(stringResource(R.string.import_download_hint), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(author, vm::setAuthor, label = { Text(stringResource(R.string.author_required)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(title, vm::setTitle, label = { Text(stringResource(R.string.title_required)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (editing == null) OutlinedTextField(magnet, vm::setMagnet, label = { Text(stringResource(R.string.magnet_link)) }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                val ready = !busy && author.isNotBlank() && title.isNotBlank() && effectiveFolder != null
                if (editing != null) {
                    Button(onClick = { requestNotifications(); vm.saveAndResume() }, enabled = ready) { Text(stringResource(R.string.save_resume)) }
                    TextButton(onClick = vm::cancelEdit) { Text(stringResource(R.string.cancel_edit)) }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { requestNotifications(); vm.startMagnet() }, enabled = ready && magnet.isNotBlank()) { Text(stringResource(R.string.start_download)) }
                        OutlinedButton(onClick = { requestNotifications(); picker.launch(arrayOf("application/x-bittorrent", "application/octet-stream")) }, enabled = ready) { Text(stringResource(R.string.open_torrent)) }
                    }
                }
            }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            error?.let { reason -> item {
                Text(stringResource(when (reason) {
                    DownloadFormError.REQUIRED -> R.string.required_download_metadata
                    DownloadFormError.FOLDER -> R.string.folder_access_error
                    DownloadFormError.ACTION -> R.string.download_action_error
                }), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = vm::dismissError) { Text(stringResource(R.string.dismiss)) }
            } }
            when (val result = state) {
                UiState.Idle, UiState.Loading -> item { CircularProgressIndicator() }
                is UiState.Error -> item { Text(stringResource(R.string.download_queue_error)) }
                is UiState.Success -> {
                    if (result.data.isEmpty()) item { Text(stringResource(R.string.no_downloads)) }
                    items(result.data, key = { it.id }) { download ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(download.title, style = MaterialTheme.typography.titleMedium)
                                download.author?.let { Text(it) }
                                Text(stringResource(statusLabel(download.status)))
                                val publishing = download.status in setOf(DownloadStatus.PUBLISHING, DownloadStatus.PUBLICATION_FAILED)
                                LinearProgressIndicator(progress = { (if (publishing) download.publicationProgress else download.progress).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                                if (publishing) Text(stringResource(R.string.publication_stats, (download.publicationProgress * 100).toInt()))
                                else Text(stringResource(R.string.download_stats, (download.progress * 100).toInt(), String.format(Locale.getDefault(), "%.1f", download.downloadSpeedBytesPerSec / 1024.0), download.peers))
                                if (download.status == DownloadStatus.PUBLICATION_FAILED) Text(stringResource(R.string.publication_retry_hint))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    when {
                                        download.status in setOf(DownloadStatus.PAUSED, DownloadStatus.ERROR, DownloadStatus.PUBLICATION_FAILED) || (download.status == DownloadStatus.COMPLETED && download.publishedFolderUri == null) ->
                                            TextButton(onClick = { vm.resume(download) }, enabled = !busy) { Text(stringResource(R.string.resume)) }
                                        download.status in setOf(DownloadStatus.QUEUED, DownloadStatus.CHECKING, DownloadStatus.DOWNLOADING) ->
                                            TextButton(onClick = { vm.pause(download.infoHash) }, enabled = !busy) { Text(stringResource(R.string.pause)) }
                                    }
                                    download.publishedFolderUri?.let { uri ->
                                        TextButton(onClick = {
                                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri), "vnd.android.document/directory").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                                                .onFailure { android.widget.Toast.makeText(context, R.string.open_folder_hint, android.widget.Toast.LENGTH_LONG).show() }
                                        }) { Text(stringResource(R.string.open_folder)) }
                                    }
                                    TextButton(onClick = { vm.cancel(download.infoHash) }, enabled = !busy) { Text(stringResource(R.string.remove_download)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun statusLabel(status: DownloadStatus): Int = when (status) {
    DownloadStatus.QUEUED -> R.string.download_queued
    DownloadStatus.CHECKING -> R.string.download_checking
    DownloadStatus.DOWNLOADING -> R.string.download_downloading
    DownloadStatus.PAUSED -> R.string.download_paused
    DownloadStatus.PUBLISHING -> R.string.download_publishing
    DownloadStatus.PUBLICATION_FAILED -> R.string.download_publication_failed
    DownloadStatus.COMPLETED, DownloadStatus.SEEDING -> R.string.download_completed
    DownloadStatus.ERROR -> R.string.download_failed
    DownloadStatus.CANCELLED -> R.string.download_cancelled
}
