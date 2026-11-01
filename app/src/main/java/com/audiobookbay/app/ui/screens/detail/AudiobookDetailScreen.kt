package com.audiobookbay.app.ui.screens.detail

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
 * Audiobook detail screen.
 *
 * Designed to support:
 * - Cover image (large, with elevation/blur shadow)
 * - Title, author, narrator, duration, format, bitrate
 * - Description (expandable)
 * - Download button with torrent info (seeders/leechers/size)
 * - Play button (if already downloaded)
 * - Chapter list & file breakdown
 * - Related recommendations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudiobookDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Audiobook Details") },
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
            Text(
                text = "Audiobook Detail View",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}
