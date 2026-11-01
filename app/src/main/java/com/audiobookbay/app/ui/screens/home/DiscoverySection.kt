package com.audiobookbay.app.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.audiobookbay.app.R
import com.audiobookbay.app.domain.model.DiscoveryPick
import androidx.compose.animation.*
import androidx.compose.animation.core.tween

@Composable
internal fun DiscoverySection(picks: List<DiscoveryPick>, hasHistory: Boolean, available: Boolean, busy: Boolean, failed: String?, generate: () -> Unit, settings: () -> Unit, detail: (String) -> Unit) {
    val shelfWidth = ((androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp - 32.dp) * 0.72f).coerceIn(220.dp, 360.dp)
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.featured_shelf), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(if (hasHistory) R.string.discovery_context else R.string.discovery_first), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!available) OutlinedButton(shape = MaterialTheme.shapes.small, onClick = settings) { Text(stringResource(R.string.discovery_setup)) }
        else Button(shape = MaterialTheme.shapes.small, onClick = generate, enabled = hasHistory && !busy) { Text(stringResource(if (busy) R.string.discovery_generating else R.string.discovery_generate)) }
        if (failed != null) Text(stringResource(when(failed) {
            "key" -> R.string.discovery_key_error
            "quota" -> R.string.discovery_quota_error
            "empty" -> R.string.discovery_no_results
            "timeout" -> R.string.discovery_timeout
            else -> R.string.discovery_failed
        }), color = MaterialTheme.colorScheme.error)
        AnimatedContent(targetState = picks, label = "Personal picks", transitionSpec = {
            (fadeIn(tween(220)) + slideInVertically(tween(220)) { 18 }) togetherWith fadeOut(tween(120))
        }) { shown ->
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        shown.firstOrNull()?.let { lead ->
            Column(Modifier.fillMaxWidth().clickable { detail(lead.book.id) }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(model = lead.book.coverImageUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(lead.book.title, style = MaterialTheme.typography.titleLarge)
                    Text(lead.book.author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(lead.reason, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(shown.drop(1), key = { it.book.source.toString() + it.book.id }) { pick ->
                Column(Modifier.width(shelfWidth).clickable { detail(pick.book.id) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AsyncImage(model = pick.book.coverImageUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                    Text(pick.book.title, style = MaterialTheme.typography.titleMedium, maxLines = 3)
                    Text(pick.book.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(pick.reason, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        }
        }
    }
}
