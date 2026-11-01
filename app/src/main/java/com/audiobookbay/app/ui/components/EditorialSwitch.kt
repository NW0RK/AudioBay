package com.audiobookbay.app.ui.components
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
@Composable fun EditorialSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val position by animateDpAsState(if (checked) 22.dp else 4.dp, tween(140), label = "Preference position")
    Box(Modifier.size(52.dp,48.dp).toggleable(checked, role = Role.Switch, onValueChange = onCheckedChange), contentAlignment = Alignment.Center) {
        Box(Modifier.size(44.dp,26.dp).clip(RoundedCornerShape(6.dp)).background(if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)) {
            Box(Modifier.offset(x = position,y = 4.dp).size(18.dp).clip(RoundedCornerShape(3.dp)).background(if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant))
        }
    }
}
