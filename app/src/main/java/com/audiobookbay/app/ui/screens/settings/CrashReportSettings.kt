package com.audiobookbay.app.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.audiobookbay.app.R
import com.audiobookbay.app.diagnostics.CrashReports
import com.audiobookbay.app.ui.components.EditorialPanel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CrashReportViewModel @Inject constructor(private val reports: CrashReports) : ViewModel() {
    private val mutableReports = MutableStateFlow<List<String>>(emptyList())
    val saved = mutableReports.asStateFlow()
    private val mutableError = MutableStateFlow(false)
    val error = mutableError.asStateFlow()
    private val mutableExporting = MutableStateFlow(false)
    val exporting = mutableExporting.asStateFlow()
    private val mutableExported = MutableStateFlow(false)
    val exported = mutableExported.asStateFlow()
    fun export(uri: Uri) { viewModelScope.launch {
        mutableExporting.value = true
        mutableExported.value = false
        mutableError.value = false
        try { reports.writeExport(uri); mutableExported.value = true }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutableError.value = true }
        finally { mutableExporting.value = false }
    } }
    init { refresh() }
    fun refresh(clear: Boolean = false, diagnostic: Boolean = false) { viewModelScope.launch {
        mutableError.value = false
        try {
            if (clear) reports.clear()
            if (diagnostic) reports.createDiagnostic()
            mutableReports.value = reports.load()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutableError.value = true }
    } }
}

@Composable
fun CrashReportSettings() {
    val vm: CrashReportViewModel = hiltViewModel()
    val reports by vm.saved.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val exporting by vm.exporting.collectAsStateWithLifecycle()
    val exported by vm.exported.collectAsStateWithLifecycle()
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) vm.export(uri)
    }
    val context = LocalContext.current
    var openError by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    EditorialPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.crashes_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.crashes_description), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.crashes_export_description), style = MaterialTheme.typography.bodySmall)
            Button(shape = MaterialTheme.shapes.small, onClick = {
                try { exporter.launch("AudioHarbour-diagnostics.txt"); openError = false }
                catch (_: Exception) { openError = true }
            }, enabled = !exporting) { Text(stringResource(R.string.crashes_export)) }
            if (exporting) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (exported) Text(stringResource(R.string.crashes_exported))
            if (reports.isEmpty()) Text(stringResource(R.string.crashes_empty))
            OutlinedButton(shape = MaterialTheme.shapes.small, onClick = { vm.refresh(diagnostic = true) }) { Text(stringResource(R.string.crashes_create)) }
            reports.forEachIndexed { index, report ->
                var expanded by remember(report) { mutableStateOf(false) }
                TextButton(shape = MaterialTheme.shapes.small, onClick = { expanded = !expanded }) { Text(stringResource(R.string.crashes_view, index + 1)) }
                if (expanded) SelectionContainer { Text(report, style = MaterialTheme.typography.bodySmall) }
                Button(shape = MaterialTheme.shapes.small, onClick = {
                    try {
                        val uri = Uri.parse("https://github.com/NW0RK/AudioBay/issues/new").buildUpon()
                            .appendQueryParameter("title", "Download crash report")
                            .appendQueryParameter("body", "Please describe what happened before the crash.\n\nSanitized report excerpt (use Copy reports for full details):\n```text\n${report.take(1400)}\n```\n")
                            .build()
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        openError = false
                    } catch (_: Exception) { openError = true }
                }) { Text(stringResource(R.string.crashes_report)) }
            }
            if (reports.isNotEmpty()) {
                OutlinedButton(shape = MaterialTheme.shapes.small, onClick = {
                    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(
                        ClipData.newPlainText("AudioHarbour crash reports", reports.joinToString("\n\n---\n\n")))
                    copied = true
                }) { Text(stringResource(R.string.crashes_copy)) }
                TextButton(shape = MaterialTheme.shapes.small, onClick = { copied = false; vm.refresh(clear = true) }) { Text(stringResource(R.string.crashes_delete)) }
            }
            TextButton(shape = MaterialTheme.shapes.small, onClick = { vm.refresh() }) { Text(stringResource(R.string.crashes_refresh)) }
            if (copied) Text(stringResource(R.string.crashes_copied))
            if (error || openError) Text(stringResource(R.string.crashes_error), color = MaterialTheme.colorScheme.error)
        }
    }
}
