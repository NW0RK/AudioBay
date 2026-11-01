package com.audiobookbay.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Settings screen.
 *
 * Designed to support:
 * - Theme selection (system default, dynamic color, light, dark)
 * - Torrent download preferences (Wi-Fi only, speed caps, max connections)
 * - Storage path selection & scoped storage management
 * - Playback defaults (default speed, seek intervals, auto-rewind on resume)
 * - Content source configuration (AudioBookBay, LibriVox, Internet Archive)
 * - Open source licenses and legal disclaimers
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") },
        )
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            ListItem(
                headlineContent = { Text("Appearance & Theme") },
                supportingContent = { Text("System default (Dynamic Color enabled)") },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Download Settings") },
                supportingContent = { Text("Wi-Fi only, App-specific storage") },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Playback Preferences") },
                supportingContent = { Text("Speed 1.0x, Skip 30s / Rewind 15s") },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Audio Sources") },
                supportingContent = { Text("AudioBookBay, LibriVox Public Domain") },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Storage Usage") },
                supportingContent = { Text("Manage cached data and audio files") },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("About") },
                supportingContent = { Text("AudioBookBay Android v0.1.0 (Alpha)") },
            )
        }
    }
}
