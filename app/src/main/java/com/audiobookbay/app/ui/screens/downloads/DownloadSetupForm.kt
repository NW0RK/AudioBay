package com.audiobookbay.app.ui.screens.downloads

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.audiobookbay.app.R
import com.audiobookbay.app.ui.components.EditorialActionButton
import com.audiobookbay.app.ui.components.EditorialPanel

internal data class DownloadSetupState(
    val title: String,
    val author: String,
    val magnet: String,
    val folderLabel: String,
    val folderSelected: Boolean,
    val catalogBook: Boolean = false,
    val editingDownload: Boolean = false,
    val expanded: Boolean = false,
    val editingMetadata: Boolean = false,
    val editingLink: Boolean = false,
    val showingFolder: Boolean = false,
    val busy: Boolean = false,
    val wifiOnly: Boolean = false,
    val error: DownloadFormError? = null,
    val correctionAvailable: Boolean = false,
    val correcting: Boolean = false,
    val correctionFailed: Boolean = false,
    val canUndoNames: Boolean = false,
) {
    val metadataMissing get() = author.isBlank() || title.isBlank()
    val ready get() = !busy && !correcting && !metadataMissing && folderSelected
}

/** All state belongs to the screen; this form only renders values and emits events. */
@Composable
internal fun DownloadSetupForm(
    state: DownloadSetupState,
    onToggleExpanded: () -> Unit,
    onToggleMetadata: () -> Unit,
    onToggleLink: () -> Unit,
    onToggleFolder: () -> Unit,
    onAuthorChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onMagnetChange: (String) -> Unit,
    onChooseFolder: () -> Unit,
    onStart: () -> Unit,
    onOpenTorrent: () -> Unit,
    onSaveResume: () -> Unit,
    onCancelEdit: () -> Unit,
    onDismissError: () -> Unit,
    onFixNames: () -> Unit = {},
    onUndoNames: () -> Unit = {},
) {
    var attempted by remember { mutableStateOf(false) }
    var validateLink by remember { mutableStateOf(true) }
    val titleFocus = remember { FocusRequester() }
    val authorFocus = remember { FocusRequester() }
    val folderFocus = remember { FocusRequester() }
    val magnetFocus = remember { FocusRequester() }
    val invalidLink = !state.magnet.trim().startsWith("magnet:?")
    fun submit(needsLink: Boolean, action: () -> Unit) {
        attempted = true
        validateLink = needsLink
        when {
            state.title.isBlank() -> titleFocus.requestFocus()
            state.author.isBlank() -> authorFocus.requestFocus()
            !state.folderSelected -> folderFocus.requestFocus()
            needsLink && invalidLink -> Unit
            else -> action()
        }
    }
    LaunchedEffect(attempted, state.magnet) {
        if (attempted && validateLink && invalidLink && !state.metadataMissing && state.folderSelected) magnetFocus.requestFocus()
    }
    val expanded = state.expanded || state.editingDownload
    EditorialPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (state.catalogBook) R.string.download_ready_heading else R.string.download_new_heading),
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(shape = MaterialTheme.shapes.small, onClick = onToggleExpanded, enabled = !state.editingDownload,
                    modifier = Modifier.testTag("toggle_download_setup")) {
                    Icon(if (expanded) Icons.Default.Remove else Icons.Default.Add,
                        stringResource(if (expanded) R.string.download_collapse else R.string.download_expand))
                }
            }
            if (expanded) {
                val showFields = state.editingMetadata || state.metadataMissing || !state.catalogBook || state.editingDownload
                if (showFields) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(state.title, onTitleChange, enabled = !state.busy,
                            label = { Text(stringResource(R.string.download_book_title)) },
                            isError = attempted && state.title.isBlank(),
                            supportingText = { if (attempted && state.title.isBlank()) Text(stringResource(R.string.field_required)) },
                            singleLine = true, modifier = Modifier.fillMaxWidth().focusRequester(titleFocus).testTag("download_title"))
                        OutlinedTextField(state.author, onAuthorChange, enabled = !state.busy,
                            label = { Text(stringResource(R.string.download_author)) },
                            isError = attempted && state.author.isBlank(),
                            supportingText = { if (attempted && state.author.isBlank()) Text(stringResource(R.string.field_required)) },
                            singleLine = true, modifier = Modifier.fillMaxWidth().focusRequester(authorFocus).testTag("download_author"))
                        if (state.metadataMissing) Text(stringResource(R.string.download_metadata_hint),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (state.editingMetadata && !state.metadataMissing && state.catalogBook && !state.editingDownload) {
                            TextButton(shape = MaterialTheme.shapes.small, onClick = onToggleMetadata, enabled = !state.busy) { Text(stringResource(R.string.download_done)) }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(state.title, style = MaterialTheme.typography.titleLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text(state.author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(shape = MaterialTheme.shapes.small, onClick = onToggleMetadata, enabled = !state.busy,
                            contentPadding = PaddingValues(0.dp), modifier = Modifier.testTag("edit_download_book")) {
                            Text(stringResource(R.string.download_edit_book))
                        }
                    }
                }
                if (state.correctionAvailable) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(shape = MaterialTheme.shapes.small, onClick = onFixNames,
                            enabled = !state.busy && !state.correcting && (state.title.isNotBlank() || state.author.isNotBlank()),
                            modifier = Modifier.testTag("fix_book_names")) {
                            Text(stringResource(R.string.fix_book_names))
                        }
                        if (state.canUndoNames) TextButton(shape = MaterialTheme.shapes.small, onClick = onUndoNames, enabled = !state.busy && !state.correcting,
                            modifier = Modifier.testTag("undo_book_names")) { Text(stringResource(R.string.undo_book_names)) }
                    }
                    if (state.correcting) {
                        LinearProgressIndicator(Modifier.fillMaxWidth().testTag("correcting_book_names"))
                        Text(stringResource(R.string.correcting_book_names), style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.correctionFailed) Text(stringResource(R.string.book_names_failed),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    if (state.canUndoNames && !state.correcting) Text(stringResource(R.string.book_names_corrected), style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FolderOpen, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.download_destination), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (state.folderSelected) state.folderLabel.trimEnd('/').substringAfterLast('/') else stringResource(R.string.choose_folder),
                                style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(shape = MaterialTheme.shapes.small, onClick = onChooseFolder, enabled = !state.busy, modifier = Modifier.focusRequester(folderFocus)) {
                            Text(stringResource(if (state.folderSelected) R.string.download_change else R.string.download_choose))
                        }
                    }
                    if (state.folderSelected) {
                        TextButton(shape = MaterialTheme.shapes.small, onClick = onToggleFolder, contentPadding = PaddingValues(0.dp)) {
                            Text(stringResource(if (state.showingFolder) R.string.download_hide_location else R.string.download_full_location))
                        }
                        if (state.showingFolder) Text(state.folderLabel, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.testTag("download_full_location"))
                    }
                }
                if ((attempted && !state.folderSelected) || state.error == DownloadFormError.FOLDER) Text(stringResource(if (state.error == DownloadFormError.FOLDER) R.string.folder_access_error else R.string.choose_folder), color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                if (!state.editingDownload) {
                    if (!state.catalogBook || state.editingLink || state.magnet.isBlank() || (attempted && validateLink && invalidLink)) {
                        OutlinedTextField(state.magnet, onMagnetChange, enabled = !state.busy,
                            label = { Text(stringResource(R.string.magnet_link)) }, singleLine = true,
                            isError = attempted && validateLink && invalidLink,
                            supportingText = { if (attempted && validateLink && invalidLink) Text(stringResource(R.string.invalid_magnet)) },
                            modifier = Modifier.fillMaxWidth().focusRequester(magnetFocus).testTag("download_magnet"))
                        if (state.catalogBook && state.editingLink && state.magnet.isNotBlank()) {
                            TextButton(shape = MaterialTheme.shapes.small, onClick = onToggleLink, enabled = !state.busy) { Text(stringResource(R.string.download_done)) }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.download_link), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            TextButton(shape = MaterialTheme.shapes.small, onClick = onToggleLink, enabled = !state.busy, modifier = Modifier.testTag("edit_download_link")) {
                                Text(stringResource(R.string.download_edit))
                            }
                        }
                    }
                }
                if (state.wifiOnly) Text(stringResource(R.string.wifi_only_enabled_hint), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.error?.let { reason ->
                    Text(stringResource(when (reason) {
                        DownloadFormError.REQUIRED -> R.string.required_download_metadata
                        DownloadFormError.FOLDER -> R.string.folder_access_error
                        DownloadFormError.ACTION -> R.string.download_action_error
                    }), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    TextButton(shape = MaterialTheme.shapes.small, onClick = onDismissError) { Text(stringResource(R.string.dismiss)) }
                }
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (state.editingDownload) {
                    Button(shape = MaterialTheme.shapes.small, onClick = { submit(false, onSaveResume) }, enabled = !state.busy && !state.correcting) { Text(stringResource(R.string.save_resume)) }
                    TextButton(shape = MaterialTheme.shapes.small, onClick = onCancelEdit, enabled = !state.busy) { Text(stringResource(R.string.cancel_edit)) }
                } else {
                    Button(shape = MaterialTheme.shapes.small, onClick = { submit(true, onStart) }, enabled = !state.busy && !state.correcting,
                        modifier = Modifier.fillMaxWidth().testTag("start_download")) {
                        if (state.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Download, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.start_download))
                    }
                    if (!state.catalogBook) OutlinedButton(shape = MaterialTheme.shapes.small, onClick = { submit(false, onOpenTorrent) }, enabled = !state.busy && !state.correcting) { Text(stringResource(R.string.open_torrent)) }
                }
            }
        }
    }
}
