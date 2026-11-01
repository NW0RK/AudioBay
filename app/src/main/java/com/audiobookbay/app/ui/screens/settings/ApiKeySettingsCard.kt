package com.audiobookbay.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.audiobookbay.app.R
import com.audiobookbay.app.data.storage.ApiKeyFailure
import com.audiobookbay.app.ui.components.GlassPanel
import com.audiobookbay.app.ui.components.SectionHeading

@Composable
fun ApiKeySettingsCard(
    input: String, configured: Boolean, busy: Boolean, saved: Boolean, failure: ApiKeyFailure?,
    onChange: (String) -> Unit, onSave: () -> Unit, onRemove: () -> Unit,
) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeading(stringResource(R.string.api_key_section))
            Text(stringResource(R.string.api_key_description), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(if (configured) R.string.api_key_configured else R.string.api_key_missing))
            OutlinedTextField(
                value = input, onValueChange = onChange,
                label = { Text(stringResource(R.string.api_key_label)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("gemini_api_key"),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave, enabled = !busy && input.isNotBlank()) { Text(stringResource(R.string.api_key_save)) }
                TextButton(onClick = onRemove, enabled = !busy && configured) { Text(stringResource(R.string.api_key_remove)) }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (failure != null) Text(stringResource(when (failure) {
                ApiKeyFailure.FORMAT -> R.string.api_key_format_error
                ApiKeyFailure.ENCRYPTION -> R.string.api_key_encryption_error
                ApiKeyFailure.STORAGE -> R.string.api_key_storage_error
            }), color = MaterialTheme.colorScheme.error)
            if (saved && failure == null) Text(stringResource(R.string.api_key_saved))
        }
    }
}
