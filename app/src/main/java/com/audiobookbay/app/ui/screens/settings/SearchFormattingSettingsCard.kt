package com.audiobookbay.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.audiobookbay.app.R
import com.audiobookbay.app.ui.components.EditorialPanel
import com.audiobookbay.app.ui.components.EditorialSwitch

@Composable
fun SearchFormattingSettingsCard(enabled: Boolean, saveFailed: Boolean, onChange: (Boolean) -> Unit) {
    EditorialPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(enabled, role = Role.Switch, onValueChange = onChange), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ai_search_format_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(end = 12.dp))
                EditorialSwitch(enabled, null)
            }
            Text(stringResource(R.string.ai_search_format_description), style = MaterialTheme.typography.bodySmall)
            if (saveFailed) Text(stringResource(R.string.ai_search_format_save_error), color = MaterialTheme.colorScheme.error)
        }
    }
}
