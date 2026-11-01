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
import com.audiobookbay.app.ui.screens.home.AudiobookCard

@Composable
fun SearchScreen(onNavigateToDetail: (String) -> Unit) {
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
        Text(stringResource(R.string.nav_search), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(vertical = 16.dp))
        Text(stringResource(R.string.search_description), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp))
        OutlinedTextField(
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.submit(); keyboard?.hide() }),
            value = query, onValueChange = vm::setQuery, singleLine = true,
            leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) { Icon(androidx.compose.material.icons.Icons.Default.Close, stringResource(R.string.clear_search)) } },
            shape = MaterialTheme.shapes.large,
            label = { Text(stringResource(R.string.search_hint)) }, modifier = Modifier.fillMaxWidth(),
        )
        if (query.isBlank() && history.isNotEmpty()) {
            com.audiobookbay.app.ui.components.SectionHeading(stringResource(R.string.recent_searches))
            TextButton(onClick = vm::clearHistory) { Text(stringResource(R.string.clear_history)) }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history) { item -> SuggestionChip(onClick = { vm.setQuery(item) }, label = { Text(item) }) }
            }
        }
        when (val result = state) {
            UiState.Idle -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(androidx.compose.material.icons.Icons.Default.TravelExplore, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.search_prompt), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
            }
            UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is UiState.Error -> Column(Modifier.padding(24.dp)) {
                Text(stringResource(R.string.discovery_error))
                Button(onClick = vm::retry) { Text(stringResource(R.string.retry)) }
            }
            is UiState.Success -> {
                if (result.data.isNotEmpty()) Text(stringResource(R.string.search_results_count, result.data.size), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp))
                if (result.data.isEmpty()) Text(stringResource(R.string.search_empty), modifier = Modifier.padding(vertical = 24.dp))
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(150.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 128.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(result.data, key = { "${it.source}:${it.id}" }) { book -> AudiobookCard(book) { onNavigateToDetail(book.id) } }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        if (loadingMore) CircularProgressIndicator()
                        else if (pageError) TextButton(onClick = vm::retry) { Text(stringResource(R.string.retry)) }
                        else if (!endReached) TextButton(onClick = vm::loadMore) { Text(stringResource(R.string.load_more)) }
                    }
                }
            }
        }
    }
}
