package com.audiobookbay.app.ui.screens.detail

import androidx.compose.foundation.layout.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.audiobookbay.app.R
import com.audiobookbay.app.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudiobookDetailScreen(onNavigateBack: () -> Unit, onNavigateToPlayer: () -> Unit) {
    val vm: DetailViewModel = hiltViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.detail_title)) },
            navigationIcon = { IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
            } },
        )
        when (val result = state) {
            UiState.Idle, UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is UiState.Error -> Column(Modifier.padding(24.dp)) {
                Text(stringResource(R.string.discovery_error))
                Button(onClick = vm::load) { Text(stringResource(R.string.retry)) }
            }
            is UiState.Success -> {
                val book = result.data
                LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = book.coverImageUrl, contentDescription = null,
                                placeholder = painterResource(R.drawable.cover_placeholder),
                                error = painterResource(R.drawable.cover_placeholder), fallback = painterResource(R.drawable.cover_placeholder),
                                contentScale = ContentScale.Fit, modifier = Modifier.size(240.dp),
                            )
                        }
                    }
                    item { Text(book.title, style = MaterialTheme.typography.headlineMedium) }
                    item { Text(book.author, style = MaterialTheme.typography.titleMedium) }
                    book.narrator?.let { narrator -> item { Text(stringResource(R.string.narrated_by, narrator)) } }
                    item {
                        Text(if (book.source == AudiobookSource.LIBRIVOX) stringResource(R.string.source_librivox) else stringResource(R.string.source_abb), color = MaterialTheme.colorScheme.primary)
                        Text(listOfNotNull(book.audioFormat, book.bitrate, book.formattedSize.takeIf { it.isNotBlank() }).joinToString(" · "))
                    }
                    item {
                        Text(book.description, maxLines = if (expanded) Int.MAX_VALUE else 7, overflow = TextOverflow.Ellipsis)
                        if (book.description.isNotBlank()) TextButton(onClick = { expanded = !expanded }) {
                            Text(stringResource(if (expanded) R.string.show_less else R.string.read_more))
                        }
                    }
                    book.torrentInfo?.let { torrent ->
                        item {
                            Text(stringResource(R.string.torrent_information), style = MaterialTheme.typography.titleMedium)
                            Text(torrent.infoHash, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    item { Text(stringResource(R.string.download_not_ready), style = MaterialTheme.typography.bodySmall) }
                    if (book.chapters.isNotEmpty()) {
                        item { Text(stringResource(R.string.chapters_count, book.chapters.size), style = MaterialTheme.typography.titleLarge) }
                        items(book.chapters, key = { it.id }) { chapter -> Text("${chapter.trackNumber}. ${chapter.title}") }
                    }
                }
            }
        }
    }
}
