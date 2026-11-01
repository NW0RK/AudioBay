package com.audiobookbay.app.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.audiobookbay.app.R
import com.audiobookbay.app.domain.model.UiState
import com.audiobookbay.app.ui.components.*

@Composable
fun SearchScreen(onNavigateToDetail: (String) -> Unit, onBrowse: () -> Unit = {}, onDownload: (String?) -> Unit = {}) {
    val actions = rememberCatalogActions()
    val vm: SearchViewModel = hiltViewModel()
    val query by vm.query.collectAsStateWithLifecycle()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val endReached by vm.endReached.collectAsStateWithLifecycle()
    val loadingMore by vm.isLoadingMore.collectAsStateWithLifecycle()
    val pageError by vm.pageError.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val gridState = rememberLazyGridState()
    val nearEnd by remember { derivedStateOf { val info = gridState.layoutInfo; info.totalItemsCount > 0 && (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 5 } }
    LaunchedEffect(nearEnd, state) { if (nearEnd && state is UiState.Success && !loadingMore && !pageError && !endReached) vm.loadMore() }
    LaunchedEffect(query) { gridState.scrollToItem(0) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        CatalogSaveError(actions)
        Text(stringResource(R.string.nav_search), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(vertical = 16.dp))
        Text(stringResource(R.string.search_description), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp))
        OutlinedTextField(
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.submit(); keyboard?.hide() }),
            value = query, onValueChange = vm::setQuery, singleLine = true,
            leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(shape = MaterialTheme.shapes.small, onClick = { vm.setQuery("") }) { Icon(androidx.compose.material.icons.Icons.Default.Close, stringResource(R.string.clear_search)) } },
            shape = MaterialTheme.shapes.large,
            label = { Text(stringResource(R.string.search_hint)) }, modifier = Modifier.fillMaxWidth(),
        )
        if (query.isBlank() && history.isNotEmpty()) {
            com.audiobookbay.app.ui.components.SectionHeading(stringResource(R.string.recent_searches))
            TextButton(shape = MaterialTheme.shapes.small, onClick = vm::clearHistory) { Text(stringResource(R.string.clear_history)) }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history) { item -> SuggestionChip(onClick = { vm.setQuery(item) }, label = { Text(item) }) }
            }
        }
        StateContent(state, Modifier.weight(1f)) { result ->
        Column(Modifier.fillMaxSize()) {
        when (result) {
            UiState.Idle -> MessageState(stringResource(R.string.search_prompt), stringResource(R.string.search_description), stringResource(R.string.browse_audiobooks), onBrowse, scrollable = true)
            UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is UiState.Error -> MessageState(stringResource(R.string.discovery_error), stringResource(R.string.catalog_error_hint), stringResource(R.string.retry), vm::retry, error = true, scrollable = true)
            is UiState.Success -> {
                if (result.data.isNotEmpty()) Text(stringResource(R.string.search_results_count, result.data.size), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp))
                if (result.data.isEmpty()) MessageState(stringResource(R.string.search_empty), stringResource(R.string.search_empty_hint), stringResource(R.string.clear_search), { vm.setQuery("") })
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(150.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(result.data, key = { "${it.source}:${it.id}" }) { book ->
                        val card = actions.state(book)
                        AudiobookCard(book, state = card, onSave = { actions.toggle(book) },
                            onDownload = { onDownload(if (card.download == null) book.id else null) }, onClick = { onNavigateToDetail(book.id) }) }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        if (loadingMore) CircularProgressIndicator()
                        else if (pageError) TextButton(shape = MaterialTheme.shapes.small, onClick = vm::retry) { Text(stringResource(R.string.retry)) }
                        else if (!endReached) TextButton(shape = MaterialTheme.shapes.small, onClick = vm::loadMore) { Text(stringResource(R.string.load_more)) }
                    }
                }
            }
        }
        }
        }
    }
}
