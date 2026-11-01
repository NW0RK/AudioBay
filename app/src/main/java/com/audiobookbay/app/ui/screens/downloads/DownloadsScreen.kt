package com.audiobookbay.app.ui.screens.downloads

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.audiobookbay.app.R
import com.audiobookbay.app.domain.model.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen() {
    val vm: DownloadsViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val error by vm.hasError.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var magnet by rememberSaveable { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.importTorrent(it.toString()) } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.nav_downloads)) })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text(stringResource(R.string.import_download_hint), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(magnet, { magnet = it }, label = { Text(stringResource(R.string.magnet_link)) }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { requestNotifications(); vm.startMagnet(magnet) }, enabled = !busy && magnet.isNotBlank()) { Text(stringResource(R.string.start_download)) }
                    OutlinedButton(onClick = { requestNotifications(); picker.launch(arrayOf("application/x-bittorrent", "application/octet-stream")) }, enabled = !busy) { Text(stringResource(R.string.open_torrent)) }
                }
            }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (error) item {
                Text(stringResource(R.string.download_action_error), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = vm::dismissError) { Text(stringResource(R.string.dismiss)) }
            }
            when (val result = state) {
                UiState.Idle, UiState.Loading -> item { CircularProgressIndicator() }
                is UiState.Error -> item { Text(stringResource(R.string.download_queue_error)) }
                is UiState.Success -> {
                    if (result.data.isEmpty()) item { Text(stringResource(R.string.no_downloads)) }
                    items(result.data, key = { it.id }) { download ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(download.title, style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(statusLabel(download.status)))
                                LinearProgressIndicator(progress = { download.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                                Text(stringResource(R.string.download_stats, (download.progress * 100).toInt(), String.format(Locale.getDefault(), "%.1f", download.downloadSpeedBytesPerSec / 1024.0), download.peers))
                                download.errorMessage?.let { Text(stringResource(R.string.download_action_error), color = MaterialTheme.colorScheme.error) }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    when (download.status) {
                                        DownloadStatus.PAUSED, DownloadStatus.ERROR -> TextButton(onClick = { vm.resume(download.infoHash) }, enabled = !busy) { Text(stringResource(R.string.resume)) }
                                        DownloadStatus.QUEUED, DownloadStatus.CHECKING, DownloadStatus.DOWNLOADING -> TextButton(onClick = { vm.pause(download.infoHash) }, enabled = !busy) { Text(stringResource(R.string.pause)) }
                                        else -> Unit
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
    DownloadStatus.COMPLETED, DownloadStatus.SEEDING -> R.string.download_completed
    DownloadStatus.ERROR -> R.string.download_failed
    DownloadStatus.CANCELLED -> R.string.download_cancelled
}
