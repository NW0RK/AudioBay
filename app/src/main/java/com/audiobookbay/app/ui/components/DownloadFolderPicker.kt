package com.audiobookbay.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.audiobookbay.app.R
import com.audiobookbay.app.data.storage.DownloadFolderSettings

@Composable
fun DownloadFolderPicker(label: String, selected: Boolean, enabled: Boolean = true, onSelect: (String) -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let { onSelect(it.toString()) } }
    Column {
        Text(stringResource(R.string.download_folder), style = MaterialTheme.typography.titleSmall)
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.folder_structure), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { picker.launch(DownloadFolderSettings.initialUri) }, enabled = enabled) {
            Text(stringResource(if (selected) R.string.change_folder else R.string.choose_folder))
        }
        if (!selected) Text(stringResource(R.string.folder_setup_hint), style = MaterialTheme.typography.bodySmall)
    }
}
