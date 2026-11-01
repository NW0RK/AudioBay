package com.audiobookbay.app.ui.screens.settings

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
import com.audiobookbay.app.ui.components.GlassPanel
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
    init { refresh() }
    fun refresh(clear: Boolean = false) { viewModelScope.launch {
        mutableError.value = false
        try {
            if (clear) reports.clear()
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
    val context = LocalContext.current
    var openError by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    GlassPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.crashes_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.crashes_description), style = MaterialTheme.typography.bodySmall)
            if (reports.isEmpty()) Text(stringResource(R.string.crashes_empty))
            reports.forEachIndexed { index, report ->
                var expanded by remember(report) { mutableStateOf(false) }
                TextButton(onClick = { expanded = !expanded }) { Text(stringResource(R.string.crashes_view, index + 1)) }
                if (expanded) SelectionContainer { Text(report, style = MaterialTheme.typography.bodySmall) }
                Button(onClick = {
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
                OutlinedButton(onClick = {
                    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(
                        ClipData.newPlainText("AudioBay crash reports", reports.joinToString("\n\n---\n\n")))
                    copied = true
                }) { Text(stringResource(R.string.crashes_copy)) }
                TextButton(onClick = { copied = false; vm.refresh(clear = true) }) { Text(stringResource(R.string.crashes_delete)) }
            }
            TextButton(onClick = { vm.refresh() }) { Text(stringResource(R.string.crashes_refresh)) }
            if (copied) Text(stringResource(R.string.crashes_copied))
            if (error || openError) Text(stringResource(R.string.crashes_error), color = MaterialTheme.colorScheme.error)
        }
    }
}
