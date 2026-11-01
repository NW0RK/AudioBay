package com.audiobookbay.app.ui.screens.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Now Playing / Full-Screen Player screen.
 *
 * Designed to support:
 * - High-resolution cover artwork
 * - Book title, author, and chapter info
 * - Audio scrub bar with elapsed & remaining time
 * - Standard controls: Play/Pause, Rewind 15s, Skip 30s
 * - Playback speed selector (0.5x to 3.0x)
 * - Sleep timer modal / bottom sheet
 * - Chapter navigation drawer/sheet
 * - Bookmark management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onNavigateBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Now Playing") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "No audiobook playing",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Select an audiobook from your library to start listening",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
