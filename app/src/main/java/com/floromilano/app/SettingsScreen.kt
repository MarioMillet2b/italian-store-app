package com.floromilano.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsScreen(
    s: UiState,
    save: (String, String) -> Unit,
    onLanguage: (String) -> Unit,
) {
    val context = LocalContext.current
    var language by remember { mutableStateOf(AppLanguage.preference(context)) }
    var url by rememberSaveable(s.url) { mutableStateOf(s.url) }
    var key by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.language),
                    style = MaterialTheme.typography.titleMedium,
                )
                listOf(
                        "system" to stringResource(R.string.language_system),
                        "es" to "Español",
                        "it" to "Italiano",
                    )
                    .forEach { (id, name) ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = language == id,
                                onClick = {
                                    language = id
                                    onLanguage(id)
                                },
                                enabled = !s.busy,
                                modifier = Modifier.testTag("language-$id"),
                            )
                            TextButton(
                                onClick = {
                                    language = id
                                    onLanguage(id)
                                },
                                enabled = !s.busy,
                            ) {
                                Text(name)
                            }
                        }
                    }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.connect_store),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.connect_help),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    url,
                    { url = it },
                    label = { Text(stringResource(R.string.store_url)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    key,
                    { key = it },
                    label = {
                        Text(
                            stringResource(
                                if (s.configured) R.string.replace_key else R.string.device_key
                            )
                        )
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.connect_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { confirm = true },
                    enabled = !s.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.verify_connection))
                }
            }
        }
        Text(
            stringResource(R.string.online_required),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (confirm)
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.connect_confirm)) },
            text = { Text(url) },
            confirmButton = {
                Button(
                    onClick = {
                        confirm = false
                        save(url, key)
                    }
                ) {
                    Text(stringResource(R.string.connect))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
}
