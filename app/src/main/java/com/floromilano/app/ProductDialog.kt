package com.floromilano.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.json.JSONObject

@Composable
internal fun ProductDialog(
    productName: String?,
    categories: List<CatalogCategory>,
    enabled: Boolean,
    close: () -> Unit,
    save: (JSONObject) -> Unit,
) {
    var categoryId by remember { mutableStateOf(null as Int?) }
    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    val attributes = remember { mutableStateListOf("Colore" to "", "Taglia" to "") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = close,
        title = {
            Text(
                productName?.let { stringResource(R.string.new_variant_for, it) }
                    ?: stringResource(R.string.new_product_draft)
            )
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (productName == null)
                    OutlinedTextField(
                        name,
                        { name = it },
                        label = { Text(stringResource(R.string.product_name)) },
                    )
                if (productName == null) CategoryChoice(categories, categoryId) { categoryId = it }
                OutlinedTextField(sku, { sku = it }, label = { Text(stringResource(R.string.sku)) })
                OutlinedTextField(
                    price,
                    { price = it },
                    label = {
                        Text(
                            if (productName == null) stringResource(R.string.price)
                            else stringResource(R.string.inherit_price)
                        )
                    },
                )
                Text(stringResource(R.string.attributes))
                attributes.indices.forEach { index ->
                    val pair = attributes[index]
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            pair.first,
                            { attributes[index] = it to pair.second },
                            label = { Text(stringResource(R.string.attribute)) },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            pair.second,
                            { attributes[index] = pair.first to it },
                            label = { Text(stringResource(R.string.value)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                TextButton(onClick = { attributes.add("" to "") }, enabled = attributes.size < 10) {
                    Text(stringResource(R.string.add_attribute))
                }
                Text(
                    stringResource(R.string.initial_stock_notice),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (error)
                    Text(
                        stringResource(R.string.check_product_fields),
                        color = MaterialTheme.colorScheme.error,
                    )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entries = attributes.filter { it.second.isNotBlank() }
                    val cents = priceCents(price)
                    error =
                        sku.isBlank() ||
                            (productName == null && name.isBlank()) ||
                            (cents == null && (productName == null || price.isNotBlank())) ||
                            entries.any { it.first.isBlank() } ||
                            entries.map { it.first.trim() }.distinct().size != entries.size
                    if (!error) {
                        val attrs = JSONObject()
                        entries.forEach { attrs.put(it.first.trim(), it.second.trim()) }
                        val payload = JSONObject().put("sku", sku.trim()).put("attributes", attrs)
                        if (productName == null)
                            payload
                                .put("name", name.trim())
                                .put("category_id", categoryId ?: JSONObject.NULL)
                        if (cents != null) payload.put("price_cents", cents)
                        save(payload)
                    }
                },
                enabled = enabled,
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.cancel)) } },
    )
}
