package com.audiobookbay.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.audiobookbay.app.R
import com.audiobookbay.app.ui.components.EditorialPanel

@Composable
fun AppearanceSettingsCard(fontPercent: Int, fontSaveFailed: Boolean, onFontPercentChange: (Int) -> Unit) {
    val fontSizeLabel = stringResource(R.string.font_size_title)
    EditorialPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            com.audiobookbay.app.ui.components.SectionHeading(stringResource(R.string.appearance_section))
            Text(stringResource(R.string.font_size_percent, fontPercent), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.font_size_description), style = MaterialTheme.typography.bodySmall)
            Slider(value = fontPercent.toFloat(), onValueChange = { onFontPercentChange(kotlin.math.round(it).toInt()) },
                valueRange = 80f..150f, steps = 69,
                modifier = Modifier.semantics { contentDescription = fontSizeLabel })
            Text(stringResource(R.string.font_size_sample), style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = { onFontPercentChange(100) }) { Text(stringResource(R.string.font_size_reset)) }
            if (fontSaveFailed) Text(stringResource(R.string.appearance_save_error), color = MaterialTheme.colorScheme.error)
        }
    }
}
