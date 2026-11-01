package com.audiobookbay.app.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.audiobookbay.app.R
import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.ui.components.*

@Composable
fun HomeScreen(onNavigateToDetail: (String) -> Unit, resetToken: Int = 0, onCategory: (String?) -> Unit = {}, onDownload: (String?) -> Unit = {}, onSettings: () -> Unit = {}) {
    val vm: HomeViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val actions = rememberCatalogActions()
    val picks by (vm.discovery?.picks ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsStateWithLifecycle()
    val history by (vm.discovery?.history ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsStateWithLifecycle(initialValue = emptyList())
    val available by (vm.discovery?.available ?: kotlinx.coroutines.flow.flowOf(false)).collectAsStateWithLifecycle(initialValue = false)
    val generating by vm.generating.collectAsStateWithLifecycle()
    val failed by vm.discoveryError.collectAsStateWithLifecycle()
    HomeScreen(state, vm::refresh, vm::loadMore, vm::retry, onNavigateToDetail, resetToken, categories, onCategory, actions, onDownload,
        discoveryHeader = { DiscoverySection(picks, history.isNotEmpty(), available, generating, failed, vm::generatePicks, onSettings, onNavigateToDetail) }, discoveryLeads = picks.isNotEmpty())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState<HomeContent>, onRefresh: () -> Unit, onLoadMore: () -> Unit, onRetry: () -> Unit,
    onNavigateToDetail: (String) -> Unit, resetToken: Int = 0,
    categories: List<Category> = emptyList(), onCategory: (String?) -> Unit = {},
    actions: CatalogActions? = null, onDownload: (String?) -> Unit = {},
    discoveryHeader: @Composable () -> Unit = {},
    discoveryLeads: Boolean = true,
) {
    val content = (state as? UiState.Success)?.data
    val grid = rememberLazyGridState()
    var handledReset by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(resetToken, content?.books?.isNotEmpty() == true) {
        if (resetToken != handledReset && content?.books?.isNotEmpty() == true) {
            grid.scrollToItem(0); handledReset = resetToken
        }
    }
    val nearEnd by remember { derivedStateOf {
        val info = grid.layoutInfo
        info.totalItemsCount > 0 && (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 3
    } }
    LaunchedEffect(nearEnd, content?.books?.size) { if (nearEnd && content != null) onLoadMore() }
    Column(Modifier.fillMaxSize()) {
        CatalogSaveError(actions)
        Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        PullToRefreshBox(isRefreshing = content?.isRefreshing == true, onRefresh = onRefresh) {
            LazyVerticalGrid(columns = GridCells.Fixed(2), state = grid, modifier = Modifier.fillMaxSize().testTag("discovery_grid"),
                contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (discoveryLeads) item(span = { GridItemSpan(maxLineSpan) }) { discoveryHeader() }
                when (state) {
                    UiState.Idle, UiState.Loading -> item(span = { GridItemSpan(maxLineSpan) }) { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    is UiState.Error -> item(span = { GridItemSpan(maxLineSpan) }) {
                        MessageState(stringResource(R.string.discovery_error), stringResource(R.string.catalog_error_hint), stringResource(R.string.retry), onRetry, error = true)
                    }
                    is UiState.Success -> {
                        if (state.data.books.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                            MessageState(stringResource(R.string.discovery_empty), stringResource(R.string.catalog_empty_hint), stringResource(R.string.retry), onRefresh)
                        } else {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column {
                                    SectionHeading(stringResource(R.string.recently_added))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(state.data.books.take(12), key = { "${it.source}:${it.id}" }) { book ->
                                            HomeCard(book, actions, onDownload, onNavigateToDetail, Modifier.width(156.dp), compact = false)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SectionHeading(stringResource(R.string.categories_title))
                        TextButton(shape = MaterialTheme.shapes.small, onClick = { onCategory(null) }) { Text(stringResource(R.string.browse_all)) }
                    }
                }
                items(categories.take(6), key = { it.slug }) { category ->
                    OutlinedButton(shape = MaterialTheme.shapes.small, onClick = { onCategory(category.slug) }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text(category.displayName) }
                }
                if (!discoveryLeads) item(span = { GridItemSpan(maxLineSpan) }) { discoveryHeader() }
                if (content != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) { SectionHeading(stringResource(R.string.catalog_shelf)) }
                    items(content.books.drop(12), key = { "${it.source}:${it.id}" }, span = { GridItemSpan(maxLineSpan) }) { book ->
                        HomeCard(book, actions, onDownload, onNavigateToDetail, Modifier.fillMaxWidth(), compact = true)
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        when {
                            content.hasError -> TextButton(shape = MaterialTheme.shapes.small, onClick = onRetry) { Text(stringResource(R.string.retry)) }
                            content.isLoadingMore -> LinearProgressIndicator(Modifier.fillMaxWidth())
                            !content.endReached -> TextButton(shape = MaterialTheme.shapes.small, onClick = onLoadMore) { Text(stringResource(R.string.load_more)) }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun HomeCard(book: Audiobook, actions: CatalogActions?, onDownload: (String?) -> Unit, onDetail: (String) -> Unit, modifier: Modifier, compact: Boolean = false) {
    val state = actions?.state(book) ?: BookCardState()
    AudiobookCard(book, modifier, compact, state, homeMetadata = true, onSave = { actions?.toggle(book) },
        onDownload = { onDownload(if (state.download == null) book.id else null) }, onClick = { onDetail(book.id) })
}
