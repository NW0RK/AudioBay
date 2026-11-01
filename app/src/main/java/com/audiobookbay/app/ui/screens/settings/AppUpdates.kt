package com.audiobookbay.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.audiobookbay.app.BuildConfig
import com.audiobookbay.app.R
import com.audiobookbay.app.domain.repository.*
import com.audiobookbay.app.ui.components.GlassPanel
import com.audiobookbay.app.ui.components.SectionHeading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppUpdatesViewModel @Inject constructor(private val updates: AppUpdateRepository) : ViewModel() {
    val state = updates.state
    fun check() { viewModelScope.launch { updates.check() } }
    fun download() = updates.download()
    fun cancel() = updates.cancelDownload()
    suspend fun installationFile() = updates.installationFile()
}

@Composable
fun AppUpdateSettings() {
    val vm: AppUpdatesViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var installError by rememberSaveable { mutableStateOf(false) }
    var waitingForPermission by rememberSaveable { mutableStateOf(false) }
    var preparing by remember { mutableStateOf(false) }
    val installer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Installer cancellation leaves the verified APK ready for another attempt.
        if (result.resultCode == android.app.Activity.RESULT_FIRST_USER) installError = true
    }
    fun launchInstaller() {
        scope.launch {
            preparing = true
            installError = false
            try {
                val file = vm.installationFile() ?: return@launch
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
                installer.launch(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { installError = true }
            finally { preparing = false }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (waitingForPermission) {
            waitingForPermission = false
            if (context.packageManager.canRequestPackageInstalls()) launchInstaller() else installError = true
        }
    }
    AppUpdateCard(state, preparing, installError, vm::check, vm::download, vm::cancel) {
        installError = false
        if (context.packageManager.canRequestPackageInstalls()) launchInstaller()
        else try {
            waitingForPermission = true
            permission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
        } catch (_: Exception) { waitingForPermission = false; installError = true }
    }
}

@Composable
fun AppUpdateCard(
    state: AppUpdateState, preparing: Boolean, installError: Boolean,
    onCheck: () -> Unit, onDownload: () -> Unit, onCancel: () -> Unit, onInstall: () -> Unit,
) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeading(stringResource(R.string.updates_title))
            Text(stringResource(R.string.updates_installed, BuildConfig.VERSION_NAME))
            Text(stringResource(R.string.updates_explanation), style = MaterialTheme.typography.bodySmall)
            state.update?.let { update ->
                Text(stringResource(R.string.updates_available, update.version), style = MaterialTheme.typography.titleMedium)
                if (update.notes.isNotBlank()) {
                    var expanded by remember(update.version) { mutableStateOf(false) }
                    TextButton(onClick = { expanded = !expanded }) { Text(stringResource(R.string.updates_notes)) }
                    if (expanded) SelectionContainer { Text(update.notes, style = MaterialTheme.typography.bodySmall) }
                }
            }
            when (state.phase) {
                UpdatePhase.CHECKING -> LinearProgressIndicator(Modifier.fillMaxWidth())
                UpdatePhase.CURRENT -> Text(stringResource(R.string.updates_current))
                UpdatePhase.DOWNLOADING -> {
                    val size = state.update?.size?.coerceAtLeast(1) ?: 1
                    LinearProgressIndicator(progress = { (state.downloaded.toFloat() / size).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.updates_progress, (state.downloaded * 100 / size).toInt()))
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.updates_cancel)) }
                }
                UpdatePhase.READY -> {
                    Text(stringResource(R.string.updates_ready))
                    Button(onClick = onInstall, enabled = !preparing) { Text(stringResource(R.string.updates_install)) }
                }
                else -> Unit
            }
            if (state.phase == UpdatePhase.AVAILABLE || state.phase == UpdatePhase.ERROR && state.update != null) {
                Button(onClick = onDownload) { Text(stringResource(R.string.updates_download)) }
            }
            if (state.phase == UpdatePhase.ERROR) Text(stringResource(updateErrorString(state.error)), color = MaterialTheme.colorScheme.error)
            if (installError) Text(stringResource(R.string.updates_install_error), color = MaterialTheme.colorScheme.error)
            if (preparing) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.phase !in setOf(UpdatePhase.DOWNLOADING, UpdatePhase.CHECKING, UpdatePhase.READY)) {
                OutlinedButton(onClick = onCheck) { Text(stringResource(R.string.updates_check)) }
            }
        }
    }
}

private fun updateErrorString(error: UpdateError?): Int = when (error) {
    UpdateError.NETWORK -> R.string.updates_network_error
    UpdateError.RATE_LIMIT -> R.string.updates_rate_error
    UpdateError.STORAGE -> R.string.updates_storage_error
    UpdateError.CHECKSUM -> R.string.updates_checksum_error
    UpdateError.PACKAGE -> R.string.updates_package_error
    UpdateError.SIGNATURE -> R.string.updates_signature_error
    UpdateError.INCOMPATIBLE -> R.string.updates_incompatible_error
    else -> R.string.updates_release_error
}

@Composable
fun AppUpdateAnnouncement(updates: AppUpdateRepository) {
    val state by updates.state.collectAsStateWithLifecycle()
    val update = state.update
    if (state.announce && update != null && state.phase == UpdatePhase.AVAILABLE) {
        AlertDialog(onDismissRequest = updates::dismissAnnouncement,
            title = { Text(stringResource(R.string.updates_available, update.version)) },
            text = { Text(stringResource(R.string.updates_announcement)) },
            confirmButton = { TextButton(onClick = { updates.dismissAnnouncement(); updates.download() }) {
                Text(stringResource(R.string.updates_download))
            } },
            dismissButton = { TextButton(onClick = updates::dismissAnnouncement) { Text(stringResource(R.string.updates_later)) } })
    }
}
