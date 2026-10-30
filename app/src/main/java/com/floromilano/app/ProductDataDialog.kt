package com.floromilano.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.json.JSONObject

@Composable
internal fun ProductDataDialog(
    variant: Variant,
    categories: List<CatalogCategory>,
    enabled: Boolean,
    close: () -> Unit,
    save: (JSONObject) -> Unit,
) {
    var categoryId by remember { mutableStateOf(variant.categoryId) }
    var name by remember { mutableStateOf(variant.name) }
    var status by remember { mutableStateOf(variant.status) }
    var expanded by remember { mutableStateOf(false) }
    val labels =
        listOf(
            "draft" to R.string.draft,
            "published" to R.string.published,
            "archived" to R.string.archived,
        )
    AlertDialog(
        onDismissRequest = close,
        title = { Text(stringResource(R.string.product_data)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    name,
                    { name = it },
                    label = { Text(stringResource(R.string.product_name)) },
                )
                CategoryChoice(categories, categoryId) { categoryId = it }
                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text(
                            stringResource(
                                labels.firstOrNull { it.first == status }?.second ?: R.string.draft
                            )
                        )
                    }
                    DropdownMenu(expanded, { expanded = false }) {
                        labels.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(stringResource(label)) },
                                onClick = {
                                    status = value
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    save(
                        JSONObject()
                            .put("name", name.trim())
                            .put("status", status)
                            .put("category_id", categoryId ?: JSONObject.NULL)
                    )
                },
                enabled = enabled && name.isNotBlank(),
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
internal fun CategoryChoice(
    categories: List<CatalogCategory>,
    selected: Int?,
    select: (Int?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(
                categories.firstOrNull { it.id == selected }?.name
                    ?: stringResource(R.string.uncategorized)
            )
        }
        DropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.uncategorized)) },
                onClick = {
                    select(null)
                    expanded = false
                },
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        select(category.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
