package com.audiobookbay.app.ui.screens.categories

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.audiobookbay.app.R
import com.audiobookbay.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(onNavigateToDetail: (String) -> Unit, onDownload: (String?) -> Unit = {}, onBack: () -> Unit = {}) {
    val vm: CategoriesViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val actions = rememberCatalogActions()
    val grid = rememberLazyGridState()
    LaunchedEffect(state.slug) { grid.scrollToItem(0) }
    BackHandler(state.slug != null) { vm.select(null) }
    Column(Modifier.fillMaxSize()) {
        CatalogSaveError(actions)
        TopAppBar(title = { Text(state.categories.firstOrNull { it.slug == state.slug }?.displayName ?: stringResource(R.string.categories_title)) },
            navigationIcon = { IconButton(shape = MaterialTheme.shapes.small, onClick = { if (state.slug != null) vm.select(null) else onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } })
        LazyVerticalGrid(columns = if (state.slug == null) GridCells.Adaptive(156.dp) else GridCells.Fixed(1), state = grid, contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.slug == null) {
                items(state.categories, key = { it.slug }) { category ->
                    FilledTonalButton(shape = MaterialTheme.shapes.small, onClick = { vm.select(category.slug) }, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)) { Text(category.displayName) }
                }
            } else {
                items(state.books, key = { "${it.source}:${it.id}" }) { book ->
                    val card = actions.state(book)
                    AudiobookCard(book, state = card, onSave = { actions.toggle(book) },
                        onDownload = { onDownload(if (card.download == null) book.id else null) }, onClick = { onNavigateToDetail(book.id) })
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                when {
                    state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                    state.failed -> MessageState(stringResource(R.string.discovery_error), stringResource(R.string.catalog_error_hint), stringResource(R.string.retry), vm::retry, error = true)
                    state.slug != null && state.books.isEmpty() -> MessageState(stringResource(R.string.discovery_empty), stringResource(R.string.category_empty_hint), stringResource(R.string.categories_title), { vm.select(null) })
                    state.slug != null && !state.endReached -> TextButton(shape = MaterialTheme.shapes.small, onClick = vm::loadMore) { Text(stringResource(R.string.load_more)) }
                }
            }
        }
    }
}
