package com.audiobookbay.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import com.audiobookbay.app.ui.components.GlassPanel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
fun HomeScreen(onNavigateToDetail: (String) -> Unit, resetToken: Int = 0) {
    val viewModel: HomeViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state, viewModel::refresh, viewModel::loadMore, viewModel::retry, onNavigateToDetail, resetToken)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState<HomeContent>,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    resetToken: Int = 0,
) {
    val content = (state as? UiState.Success)?.data
    val gridState = rememberLazyGridState()
    var handledResetToken by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(resetToken, content?.books?.isNotEmpty() == true) {
        if (resetToken != handledResetToken && content?.books?.isNotEmpty() == true) {
            gridState.scrollToItem(0)
            handledResetToken = resetToken
        }
    }
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
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(stringResource(R.string.discover_title), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            }
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(androidx.compose.material.icons.Icons.Default.AutoStories, null, Modifier.padding(16.dp))
            }
        }
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
                            modifier = Modifier.testTag("discovery_grid"),
                            columns = GridCells.Adaptive(150.dp),
                            state = gridState,
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 128.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                GlassPanel {
                                    Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), Color.Transparent))).padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(stringResource(R.string.discovery_hero), style = MaterialTheme.typography.headlineMedium)
                                        Text(stringResource(R.string.discover_subtitle), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(stringResource(R.string.featured_shelf), style = MaterialTheme.typography.headlineSmall)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        items(state.data.books.take(8), key = { it.id }) { book ->
                                            Column(Modifier.width(160.dp).clickable { onNavigateToDetail(book.id) }) {
                                                AsyncImage(model = book.coverImageUrl, contentDescription = book.title,
                                                    error = painterResource(R.drawable.cover_placeholder), fallback = painterResource(R.drawable.cover_placeholder),
                                                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(0.68f).clip(RoundedCornerShape(16.dp)))
                                                Text(book.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                                            }
                                        }
                                    }
                                    Text(stringResource(R.string.catalog_shelf), style = MaterialTheme.typography.headlineSmall)
                                }
                            }
                            items(state.data.books, key = { "${it.source}:${it.id}" }, span = { GridItemSpan(maxLineSpan) }) { book ->
                                Row(Modifier.fillMaxWidth().clickable { onNavigateToDetail(book.id) }.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = book.coverImageUrl, contentDescription = null,
                                        error = painterResource(R.drawable.cover_placeholder), fallback = painterResource(R.drawable.cover_placeholder),
                                        contentScale = ContentScale.Crop, modifier = Modifier.width(88.dp).height(128.dp).clip(RoundedCornerShape(12.dp)))
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(book.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text(book.author, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        com.audiobookbay.app.ui.screens.saved.BookSummaryMetadata(book)
                                    }
                                    com.audiobookbay.app.ui.screens.saved.SaveBookControl(book)
                                }
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
    GlassPanel(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
      Column {
        AsyncImage(
            model = book.coverImageUrl,
            contentDescription = null,
            placeholder = painterResource(R.drawable.cover_placeholder),
            error = painterResource(R.drawable.cover_placeholder),
            fallback = painterResource(R.drawable.cover_placeholder),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.85f),
        )
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(book.author, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            com.audiobookbay.app.ui.screens.saved.BookSummaryMetadata(book)
            com.audiobookbay.app.ui.screens.saved.SaveBookControl(book)
        }
    }
    }
}
