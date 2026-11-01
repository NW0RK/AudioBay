package com.audiobookbay.app.ui.screens.library

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
 * Local library screen.
 *
 * Designed to support:
 * - Grid / list toggle for downloaded audiobooks
 * - Sorting by title, author, date added, listening progress
 * - Filter by completed / in-progress
 * - Progress indicator bar on covers
 * - Quick play button on book cards
 * - Long-press context menu (details, delete downloaded files)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToPlayer: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("My Library") },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Your audiobook library is empty",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
