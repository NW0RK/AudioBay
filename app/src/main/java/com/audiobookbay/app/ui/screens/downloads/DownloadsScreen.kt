package com.audiobookbay.app.ui.screens.downloads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Downloads management screen.
 *
 * Designed to support:
 * - Active downloads list with linear progress indicators
 * - Download speed (KB/s, MB/s) and ETA
 * - Peer/seeder counts
 * - Pause, resume, and cancel controls
 * - Completed downloads section
 * - Storage usage indicator & Wi-Fi status banner
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Downloads") },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No active downloads",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
