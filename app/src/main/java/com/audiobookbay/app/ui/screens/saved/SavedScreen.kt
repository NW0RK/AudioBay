package com.audiobookbay.app.ui.screens.saved
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.audiobookbay.app.R
import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.ui.screens.home.AudiobookCard
@Composable
fun SaveBookControl(book: Audiobook, vm: SavedViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val saved = (state as? UiState.Success)?.data?.any { it.id == book.id && it.source == book.source } == true
    val failed by vm.failedBook.collectAsStateWithLifecycle()
    Column {
        IconButton(onClick = { vm.change(book, !saved) }, enabled = state is UiState.Success) {
            Icon(if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                stringResource(if (saved) R.string.unsave_book else R.string.save_book, book.title))
        }
        if (failed?.id == book.id && failed?.source == book.source) {
            Text(stringResource(R.string.saved_update_error), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = vm::retryChange) { Text(stringResource(R.string.retry)) }
        }
        if (state is UiState.Error) TextButton(onClick = vm::retryLoad) { Text(stringResource(R.string.saved_load_retry)) }
    }
}
@Composable
fun BookSummaryMetadata(book: Audiobook) {
    val summary = listOfNotNull(book.language, book.audioFormat, book.formattedSize).filter { it.isNotBlank() }.joinToString(" · ")
    if (summary.isNotEmpty()) Text(summary, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
}
@Composable
fun SavedScreen(onNavigateToDetail: (String) -> Unit) {
    val vm: SavedViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.nav_saved), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(vertical = 16.dp))
        when (val result = state) {
            UiState.Idle, UiState.Loading -> CircularProgressIndicator()
            is UiState.Error -> {
                Text(stringResource(R.string.saved_load_error))
                Button(onClick = vm::retryLoad) { Text(stringResource(R.string.retry)) }
            }
            is UiState.Success -> {
                if (result.data.isEmpty()) Text(stringResource(R.string.saved_empty))
                LazyColumn(contentPadding = PaddingValues(bottom = 128.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(result.data, key = { "${it.source}:${it.id}" }) { book ->
                        AudiobookCard(book) { onNavigateToDetail(book.id) }
                    }
                }
            }
        }
    }
}
