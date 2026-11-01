package com.audiobookbay.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.audiobookbay.app.R
import com.audiobookbay.app.domain.model.*
import com.audiobookbay.app.ui.screens.saved.BookSummaryMetadata
import com.audiobookbay.app.ui.screens.downloads.statusLabel
import com.audiobookbay.app.ui.theme.LocalSemanticColors

@Composable
fun MessageState(heading: String, message: String, action: String, onAction: () -> Unit, modifier: Modifier = Modifier, error: Boolean = false, scrollable: Boolean = false) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val opacity by animateFloatAsState(if (visible) 1f else 0f, tween(200), label = "Message appearance")
    val scrollModifier = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
    Column(modifier.then(scrollModifier).graphicsLayer { alpha = opacity }.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(if (error) Icons.Outlined.CloudOff else Icons.Outlined.AutoStories, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Text(heading, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onAction) { Text(action) }
    }
}

enum class DownloadTone { Neutral, Success, Warning, Error }
fun downloadTone(status: DownloadStatus): DownloadTone = when (status) {
    DownloadStatus.COMPLETED, DownloadStatus.SEEDING -> DownloadTone.Success
    DownloadStatus.PAUSED, DownloadStatus.WAITING_FOR_WIFI -> DownloadTone.Warning
    DownloadStatus.ERROR, DownloadStatus.PUBLICATION_FAILED -> DownloadTone.Error
    else -> DownloadTone.Neutral
}

@Composable
fun DownloadStatusBadge(status: DownloadStatus) {
    val tokens = LocalSemanticColors.current
    val tone = downloadTone(status)
    val colors = when (tone) {
        DownloadTone.Success -> tokens.success
        DownloadTone.Warning -> tokens.warning
        DownloadTone.Error -> tokens.error
        DownloadTone.Neutral -> com.audiobookbay.app.ui.theme.StatusColors(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
    }
    Surface(color = colors.container, contentColor = colors.onContainer, shape = MaterialTheme.shapes.small) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(when (tone) {
                DownloadTone.Success -> Icons.Outlined.CheckCircle
                DownloadTone.Warning -> Icons.Outlined.PauseCircle
                DownloadTone.Error -> Icons.Outlined.ErrorOutline
                DownloadTone.Neutral -> Icons.Outlined.FileDownload
            }, null, Modifier.size(18.dp))
            Text(stringResource(statusLabel(status)), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun DownloadProgress(download: DownloadItem, modifier: Modifier = Modifier) {
    val publishing = download.status == DownloadStatus.PUBLISHING || download.status == DownloadStatus.PUBLICATION_FAILED
    val target = (if (publishing) download.publicationProgress else download.progress).let { if (it.isFinite()) it.coerceIn(0f, 1f) else 0f }
    val progress by animateFloatAsState(target, tween(250), label = "Download progress")
    val label = stringResource(if (publishing) R.string.publication_progress_label else R.string.download_progress_label)
    LinearProgressIndicator(progress = { progress }, modifier = modifier.semantics { contentDescription = label }.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.small))
}

data class BookCardState(val saved: Boolean = false, val saveEnabled: Boolean = false, val saveFailed: Boolean = false, val download: DownloadItem? = null)

/** All data and callbacks are supplied by the screen; no repository subscriptions per card. */
@Composable
fun AudiobookCard(
    book: Audiobook,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    state: BookCardState = BookCardState(),
    onSave: () -> Unit = {},
    onDownload: () -> Unit = {},
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, tween(140), label = "Cover press")
    val alpha by animateFloatAsState(if (pressed) 0.90f else 1f, tween(140), label = "Cover feedback")
    GlassPanel(modifier.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)) {
        if (compact) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BookCover(book, Modifier.width(88.dp).height(128.dp))
                BookCardText(book, state, onSave, onDownload, Modifier.weight(1f), true)
            }
        } else {
            Column {
                Box {
                    BookCover(book, Modifier.fillMaxWidth().aspectRatio(0.75f))
                    state.download?.let { DownloadProgress(it, Modifier.align(Alignment.BottomCenter)) }
                }
                BookCardText(book, state, onSave, onDownload, Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
private fun BookCover(book: Audiobook, modifier: Modifier) {
    AsyncImage(model = book.coverImageUrl, contentDescription = null,
        placeholder = painterResource(R.drawable.cover_placeholder), error = painterResource(R.drawable.cover_placeholder), fallback = painterResource(R.drawable.cover_placeholder),
        contentScale = ContentScale.Crop, modifier = modifier.clip(RoundedCornerShape(12.dp)))
}
@Composable
private fun BookCardText(book: Audiobook, state: BookCardState, onSave: () -> Unit, onDownload: () -> Unit, modifier: Modifier, compact: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(book.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        BookSummaryMetadata(book)
        state.download?.let {
            DownloadStatusBadge(it.status)
            if (compact) DownloadProgress(it)
        }
        Row {
            IconButton(onClick = onSave, enabled = state.saveEnabled) {
                Icon(if (state.saved) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    stringResource(if (state.saved) R.string.unsave_book else R.string.save_book, book.title))
            }
            IconButton(onClick = onDownload) {
                Icon(Icons.Outlined.FileDownload, stringResource(if (state.download != null) R.string.view_book_download else R.string.prepare_book_download, book.title))
            }
        }
        if (state.saveFailed) {
            Text(stringResource(R.string.saved_update_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onSave) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
fun CatalogSaveError(actions: CatalogActions?) {
    if (actions?.loadFailed == true) TextButton(onClick = actions.retryLoad) { Text(stringResource(R.string.saved_load_retry)) }
}

@Composable
fun <T> StateContent(state: UiState<T>, modifier: Modifier = Modifier, content: @Composable (UiState<T>) -> Unit) {
    AnimatedContent(targetState = state, modifier = modifier,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
        contentKey = { it::class }, label = "Catalog state") { current -> content(current) }
}
