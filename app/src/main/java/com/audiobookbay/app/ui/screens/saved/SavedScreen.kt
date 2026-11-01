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
import com.audiobookbay.app.ui.components.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
@Composable
fun SaveBookControl(book: Audiobook, vm: SavedViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val saved = (state as? UiState.Success)?.data?.any { it.id == book.id && it.source == book.source } == true
    val failed by vm.failedBook.collectAsStateWithLifecycle()
    Column {
        IconButton(shape = MaterialTheme.shapes.small, onClick = { vm.change(book, !saved) }, enabled = state is UiState.Success) {
            Icon(if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                stringResource(if (saved) R.string.unsave_book else R.string.save_book, book.title))
        }
        if (failed?.id == book.id && failed?.source == book.source) {
            Text(stringResource(R.string.saved_update_error), style = MaterialTheme.typography.bodySmall)
            TextButton(shape = MaterialTheme.shapes.small, onClick = vm::retryChange) { Text(stringResource(R.string.retry)) }
        }
        if (state is UiState.Error) TextButton(shape = MaterialTheme.shapes.small, onClick = vm::retryLoad) { Text(stringResource(R.string.saved_load_retry)) }
    }
}
@Composable
fun BookSummaryMetadata(book: Audiobook) {
    val summary = listOfNotNull(book.language, book.audioFormat, book.formattedSize).filter { it.isNotBlank() }.joinToString(" · ")
    if (summary.isNotEmpty()) Text(summary, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
}
enum class SavedSort { Recent, Title, Author }
fun sortSavedBooks(books: List<Audiobook>, sort: SavedSort): List<Audiobook> = when (sort) {
    SavedSort.Recent -> books
    SavedSort.Title -> books.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    SavedSort.Author -> books.sortedWith(compareBy<Audiobook, String>(String.CASE_INSENSITIVE_ORDER) { it.author }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title })
}
@Composable
fun SavedScreen(onBrowse: () -> Unit = {}, onDownload: (String?) -> Unit = {}, onNavigateToDetail: (String) -> Unit) {
    val vm: SavedViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val actions = rememberCatalogActions(savedVm = vm)
    SavedContent(state, vm::retryLoad, onBrowse, actions, onDownload, onNavigateToDetail)
}
@Composable
fun SavedContent(state: UiState<List<Audiobook>>, onRetry: () -> Unit, onBrowse: () -> Unit,
    actions: CatalogActions? = null, onDownload: (String?) -> Unit = {}, onNavigateToDetail: (String) -> Unit = {}) {
    var grid by rememberSaveable { mutableStateOf(true) }
    var sort by rememberSaveable { mutableStateOf(SavedSort.Recent) }
    var sortMenu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.nav_saved), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(vertical = 16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box {
                TextButton(shape = MaterialTheme.shapes.small, onClick = { sortMenu = true }) {
                    Text(stringResource(when (sort) { SavedSort.Recent -> R.string.sort_recent; SavedSort.Title -> R.string.sort_title; SavedSort.Author -> R.string.sort_author }))
                }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    SavedSort.entries.forEach { option ->
                        DropdownMenuItem(text = { Text(stringResource(when (option) { SavedSort.Recent -> R.string.sort_recent; SavedSort.Title -> R.string.sort_title; SavedSort.Author -> R.string.sort_author })) },
                            onClick = { sort = option; sortMenu = false })
                    }
                }
            }
            Row {
                IconToggleButton(checked = grid, onCheckedChange = { grid = true }) { Icon(Icons.Default.GridView, stringResource(R.string.layout_grid)) }
                IconToggleButton(checked = !grid, onCheckedChange = { grid = false }) { Icon(Icons.Default.ViewList, stringResource(R.string.layout_list)) }
            }
        }
        StateContent(state, Modifier.weight(1f)) { result ->
        Column(Modifier.fillMaxSize()) {
        when (result) {
            UiState.Idle, UiState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            is UiState.Error -> MessageState(stringResource(R.string.saved_load_error), stringResource(R.string.catalog_error_hint), stringResource(R.string.retry), onRetry, error = true, scrollable = true)
            is UiState.Success -> {
                if (result.data.isEmpty()) {
                    MessageState(stringResource(R.string.saved_empty), stringResource(R.string.saved_empty_hint), stringResource(R.string.browse_audiobooks), onBrowse, scrollable = true)
                } else {
                    val books = remember(result.data, sort) { sortSavedBooks(result.data, sort) }
                    LazyVerticalGrid(columns = if (grid) GridCells.Adaptive(156.dp) else GridCells.Fixed(1),
                        contentPadding = PaddingValues(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(books, key = { it.source.toString() + ":" + it.id }) { book ->
                            val card = actions?.state(book) ?: BookCardState()
                            AudiobookCard(book, compact = !grid, state = card, onSave = { actions?.toggle(book) },
                                onDownload = { onDownload(if (card.download == null) book.id else null) }, onClick = { onNavigateToDetail(book.id) })
                        }
                    }
                }
            }
        }
        }
        }
    }
}
