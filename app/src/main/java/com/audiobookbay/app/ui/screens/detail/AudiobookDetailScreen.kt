package com.audiobookbay.app.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Download
import com.audiobookbay.app.ui.components.EditorialPanel
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.testTag
import com.audiobookbay.app.ui.components.MessageState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.audiobookbay.app.R
import com.audiobookbay.app.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudiobookDetailScreen(onNavigateBack: () -> Unit, onDownload: (String) -> Unit) {
    val vm: DetailViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    AudiobookDetailContent(state, vm::load, onNavigateBack, onDownload) { book ->
        com.audiobookbay.app.ui.screens.saved.SaveBookControl(book.toSummary())
    }
}
@Composable
fun AudiobookDetailContent(state: UiState<AudiobookDetail>, onRetry: () -> Unit, onNavigateBack: () -> Unit,
    onDownload: (String) -> Unit, savedControl: @Composable (AudiobookDetail) -> Unit = {}) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        when (val result = state) {
            UiState.Idle, UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is UiState.Error -> MessageState(stringResource(R.string.discovery_error), stringResource(R.string.catalog_error_hint), stringResource(R.string.retry), onRetry, Modifier.padding(top = 64.dp), error = true)
            is UiState.Success -> {
                val book = result.data

                val list = rememberLazyListState()
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(state = list, modifier = Modifier.testTag("detail_content").fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    item {
                        var ratio by remember(book.coverImageUrl) { mutableFloatStateOf(1f) }
                        AsyncImage(model = book.coverImageUrl, contentDescription = null,
                            placeholder = painterResource(R.drawable.cover_placeholder),
                            error = painterResource(R.drawable.cover_placeholder), fallback = painterResource(R.drawable.cover_placeholder),
                            onSuccess = { result ->
                                val image = result.result.drawable
                                if (image.intrinsicWidth > 0 && image.intrinsicHeight > 0) ratio = image.intrinsicWidth.toFloat() / image.intrinsicHeight
                            },
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.testTag("detail_cover").fillMaxWidth().aspectRatio(ratio))
                    }
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(Modifier.height(20.dp))
                            Text(book.title, style = MaterialTheme.typography.headlineLarge,
                                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(book.author, style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            if (book.torrentInfo?.magnetUri == null) {
                                Spacer(Modifier.height(12.dp))
                                Text(stringResource(R.string.no_torrent_link), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item { savedControl(book) }
                    val narrators = splitNarrators(book.narrator)
                    item {
                        DetailPanel {
                            if (narrators.isNotEmpty()) {
                                Text(stringResource(R.string.narrated_by, narrators.joinToString("\n", prefix = "\n")),
                                    style = MaterialTheme.typography.bodyLarge)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            Text(if (book.source == AudiobookSource.LIBRIVOX) stringResource(R.string.source_librivox) else stringResource(R.string.source_abb),
                                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            listOf(
                                R.string.metadata_language to book.language,
                                R.string.metadata_posted to book.datePosted,
                                R.string.metadata_uploader to book.uploader,
                                R.string.metadata_format to book.audioFormat,
                                R.string.metadata_bitrate to book.bitrate,
                                R.string.metadata_size to book.formattedSize,
                            ).forEach { (label, value) ->
                                if (!value.isNullOrBlank()) Text(stringResource(label, value),
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    item {
                        DetailPanel {
                            Text(stringResource(R.string.description_heading), style = MaterialTheme.typography.titleLarge)
                            Text(book.description, style = MaterialTheme.typography.bodyLarge,
                                maxLines = if (expanded) Int.MAX_VALUE else 7, overflow = TextOverflow.Ellipsis)
                            if (book.description.isNotBlank()) TextButton(shape = MaterialTheme.shapes.small, onClick = { expanded = !expanded }) {
                                Text(stringResource(if (expanded) R.string.show_less else R.string.read_more))
                            }
                        }
                    }
                    book.torrentInfo?.let { torrent ->
                        item {
                            DetailPanel {
                                Text(stringResource(R.string.torrent_information), style = MaterialTheme.typography.titleLarge)
                                Text(torrent.infoHash, modifier = Modifier.fillMaxWidth(), softWrap = true,
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (book.source == AudiobookSource.AUDIOBOOKBAY) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(stringResource(R.string.comments_title, book.comments.size), style = MaterialTheme.typography.titleLarge)
                                TextButton(shape = MaterialTheme.shapes.small, onClick = onRetry) { Text(stringResource(R.string.refresh_comments)) }
                                if (book.comments.isEmpty()) Text(stringResource(R.string.no_comments), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        items(book.comments, key = { it.id }) { comment ->
                            EditorialPanel(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(comment.author.ifBlank { stringResource(R.string.comment_reader) }, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    if (comment.date.isNotBlank()) Text(comment.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    androidx.compose.foundation.text.selection.SelectionContainer { Text(comment.text, style = MaterialTheme.typography.bodyMedium) }
                                }
                            }
                        }
                    }
                    if (book.chapters.isNotEmpty()) {
                        item { Text(stringResource(R.string.chapters_count, book.chapters.size), style = MaterialTheme.typography.titleLarge) }
                        items(book.chapters, key = { it.id }) { chapter -> Text("${chapter.trackNumber}. ${chapter.title}") }
                    }
                }
                    if (list.firstVisibleItemIndex > 0) Box(Modifier.fillMaxWidth().height(64.dp).background(MaterialTheme.colorScheme.surface)) {
                        Text(book.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.Center).padding(horizontal = 72.dp), style = MaterialTheme.typography.titleMedium)
                    }
                    if (book.torrentInfo?.magnetUri != null) FilledIconButton(onClick = { onDownload(book.id) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                        Icon(Icons.Default.Download, stringResource(R.string.start_download))
                    }
                }
            }
        }
        Box(
            modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 8.dp).size(44.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surface)

                .clickable { onNavigateBack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun DetailPanel(content: @Composable ColumnScope.() -> Unit) {
    EditorialPanel(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
