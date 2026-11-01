package com.audiobookbay.app.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.Download
import com.audiobookbay.app.ui.components.GlassActionButton
import com.audiobookbay.app.ui.components.GlassPanel
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        when (val result = state) {
            UiState.Idle, UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is UiState.Error -> Column(Modifier.padding(horizontal = 24.dp).padding(top = 64.dp)) {
                Text(stringResource(R.string.discovery_error))
                Button(onClick = vm::load) { Text(stringResource(R.string.retry)) }
            }
            is UiState.Success -> {
                val book = result.data
                var coverAspect by remember(book.coverImageUrl) { mutableFloatStateOf(0.78f) }
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 128.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.fillMaxWidth()) {
                                Box(Modifier.fillMaxWidth().padding(bottom = if (book.torrentInfo?.magnetUri != null) 76.dp else 0.dp)
                                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                                    .background(MaterialTheme.colorScheme.surface)) {
                                    AsyncImage(
                                        model = book.coverImageUrl, contentDescription = book.title,
                                        placeholder = painterResource(R.drawable.cover_placeholder),
                                        error = painterResource(R.drawable.cover_placeholder), fallback = painterResource(R.drawable.cover_placeholder),
                                        contentScale = ContentScale.Fit,
                                        onSuccess = { image ->
                                            val size = image.painter.intrinsicSize
                                            if (size.width > 0 && size.height > 0) coverAspect = size.width / size.height
                                        },
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).aspectRatio(coverAspect),
                                    )
                                    Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(
                                        androidx.compose.ui.graphics.Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.25f),
                                    ))))
                                }
                                if (book.torrentInfo?.magnetUri != null) {
                                    GlassActionButton(Icons.Default.Download, stringResource(R.string.start_download),
                                        onClick = { onDownload(book.id) }, coverUrl = book.coverImageUrl,
                                        subtleArtwork = true,
                                        modifier = Modifier.align(Alignment.BottomCenter))
                                }
                            }
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
                    item { com.audiobookbay.app.ui.screens.saved.SaveBookControl(book.toSummary()) }
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
                            Text(book.description, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                                maxLines = if (expanded) Int.MAX_VALUE else 7, overflow = TextOverflow.Ellipsis)
                            if (book.description.isNotBlank()) TextButton(onClick = { expanded = !expanded }) {
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
                                TextButton(onClick = vm::load) { Text(stringResource(R.string.refresh_comments)) }
                                if (book.comments.isEmpty()) Text(stringResource(R.string.no_comments), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        items(book.comments, key = { it.id }) { comment ->
                            GlassPanel(Modifier.fillMaxWidth()) {
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

            }
        }
        FilledIconButton(
            onClick = onNavigateBack,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 8.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
        }
    }
}

@Composable
private fun DetailPanel(content: @Composable ColumnScope.() -> Unit) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
