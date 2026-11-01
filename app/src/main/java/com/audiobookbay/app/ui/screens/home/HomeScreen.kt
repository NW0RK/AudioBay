package com.audiobookbay.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.audiobookbay.app.R
import com.audiobookbay.app.domain.model.Audiobook
import com.audiobookbay.app.domain.model.AudiobookSource
import com.audiobookbay.app.domain.model.UiState

@Composable
fun HomeScreen(onNavigateToDetail: (String) -> Unit) {
    val viewModel: HomeViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state, viewModel::refresh, viewModel::loadMore, viewModel::retry, onNavigateToDetail)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState<HomeContent>,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
) {
    val content = (state as? UiState.Success)?.data
    val gridState = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val layout = gridState.layoutInfo
            layout.totalItemsCount > 0 &&
                (layout.visibleItemsInfo.lastOrNull()?.index ?: 0) >= layout.totalItemsCount - 5
        }
    }
    LaunchedEffect(nearEnd, content?.books?.size) {
        if (nearEnd && content != null) onLoadMore()
    }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.discover_title)) })
        PullToRefreshBox(
            isRefreshing = content?.isRefreshing == true,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when (state) {
                UiState.Idle, UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is UiState.Error -> HomeMessage(R.string.discovery_error, onRetry)
                is UiState.Success -> {
                    if (state.data.books.isEmpty()) {
                        HomeMessage(R.string.discovery_empty, onRefresh)
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = gridState,
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(stringResource(R.string.discover_subtitle), style = MaterialTheme.typography.bodyLarge)
                            }
                            items(state.data.books, key = { "${it.source}:${it.id}" }) { book ->
                                AudiobookCard(book) { onNavigateToDetail(book.id) }
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                when {
                                    state.data.hasError -> Column {
                                        Text(stringResource(R.string.discovery_error))
                                        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                                    }
                                    state.data.isLoadingMore -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                    !state.data.endReached -> TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) {
                                        Text(stringResource(R.string.load_more))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMessage(message: Int, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(message), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
internal fun AudiobookCard(book: Audiobook, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = book.coverImageUrl,
            contentDescription = null,
            placeholder = painterResource(R.drawable.cover_placeholder),
            error = painterResource(R.drawable.cover_placeholder),
            fallback = painterResource(R.drawable.cover_placeholder),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(book.author, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (book.source == AudiobookSource.LIBRIVOX) {
                Text(stringResource(R.string.source_librivox), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
