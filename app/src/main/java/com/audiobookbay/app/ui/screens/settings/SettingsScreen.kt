package com.audiobookbay.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import com.audiobookbay.app.ui.components.GlassPanel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.audiobookbay.app.R
import com.audiobookbay.app.ui.components.DownloadFolderPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val vm: SettingsViewModel = hiltViewModel()
    val folder by vm.folder.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val wifiOnly by vm.wifiOnly.collectAsStateWithLifecycle()
    val networkError by vm.networkError.collectAsStateWithLifecycle()
    val keyConfigured by vm.keyConfigured.collectAsStateWithLifecycle()
    val keyBusy by vm.keyBusy.collectAsStateWithLifecycle()
    val keyError by vm.keyError.collectAsStateWithLifecycle()
    val keyFailure by vm.keyFailure.collectAsStateWithLifecycle()
    val keySaved by vm.keySaved.collectAsStateWithLifecycle()
    // Credentials must not enter saved instance state or be displayed after saving.
    var apiKey by remember { mutableStateOf("") }
    LaunchedEffect(keySaved) { if (keySaved) apiKey = "" }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.nav_settings)) })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 112.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GlassPanel(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            com.audiobookbay.app.ui.components.SectionHeading(stringResource(R.string.storage_section))
            DownloadFolderPicker(vm.label(folder), folder != null, !busy, vm::select)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (error) Text(stringResource(R.string.folder_access_error), color = MaterialTheme.colorScheme.error)
            } }
            GlassPanel(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            com.audiobookbay.app.ui.components.SectionHeading(stringResource(R.string.network_section))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.wifi_only_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.wifi_only_description), style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = wifiOnly, onCheckedChange = vm::setWifiOnly)
            }
            if (networkError) Text(stringResource(R.string.network_settings_error), color = MaterialTheme.colorScheme.error)
            } }
            ApiKeySettingsCard(apiKey, keyConfigured, keyBusy, keySaved,
                if (keyError) keyFailure ?: com.audiobookbay.app.data.storage.ApiKeyFailure.STORAGE else null,
                onChange = { apiKey = it }, onSave = { vm.saveApiKey(apiKey) },
                onRemove = { vm.saveApiKey("") })
            AppUpdateSettings()
            CrashReportSettings()
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            com.audiobookbay.app.ui.components.SectionHeading(stringResource(R.string.about_downloads))
            Text(stringResource(R.string.external_player_hint))
            Text(stringResource(R.string.shared_files_retained), style = MaterialTheme.typography.bodySmall)
            } }
        }
    }
}
