package com.audiobookbay.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/** A translucent highlight and a fine rim keep panels light against the plum canvas. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit,
) {
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
        .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.015f))))
        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.035f))), shape)) {
        content()
    }
}

/** Blur only the artwork, leaving the icon and accessible touch target crisp. */
@Composable
fun GlassActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    coverUrl: String? = null,
    enabled: Boolean = true,
    subtleArtwork: Boolean = false,
) {
    Box(modifier.size(88.dp).clip(CircleShape)
        .background(MaterialTheme.colorScheme.surface)
        .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center) {
        AsyncImage(model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize().blur(if (subtleArtwork) 6.dp else 24.dp))
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = if (subtleArtwork) 0.12f else 0.42f),
            MaterialTheme.colorScheme.surface.copy(alpha = if (subtleArtwork) 0.58f else 0.82f),
        ))))
        Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent)))
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.06f))), CircleShape))
        Icon(icon, label, Modifier.size(32.dp), tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
    }
}

@Composable
fun SectionHeading(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun StatusBadge(text: String, error: Boolean = false) {
    Surface(color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
    }
}
