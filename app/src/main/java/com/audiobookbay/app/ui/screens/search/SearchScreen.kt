package com.audiobookbay.app.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
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
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.nav_search), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(vertical = 16.dp))
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery, singleLine = true,
            label = { Text(stringResource(R.string.search_hint)) }, modifier = Modifier.fillMaxWidth(),
        )
        if (query.isBlank() && history.isNotEmpty()) {
            TextButton(onClick = vm::clearHistory) { Text(stringResource(R.string.clear_history)) }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history) { item -> SuggestionChip(onClick = { vm.setQuery(item) }, label = { Text(item) }) }
            }
        }
        when (val result = state) {
            UiState.Idle -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.search_prompt)) }
            UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is UiState.Error -> Column(Modifier.padding(24.dp)) {
                Text(stringResource(R.string.discovery_error))
                Button(onClick = vm::retry) { Text(stringResource(R.string.retry)) }
            }
            is UiState.Success -> {
                if (result.data.isEmpty()) Text(stringResource(R.string.search_empty), modifier = Modifier.padding(vertical = 24.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2), contentPadding = PaddingValues(vertical = 16.dp),
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
