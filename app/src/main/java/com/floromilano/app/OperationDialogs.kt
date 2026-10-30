package com.floromilano.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONObject

@Composable
internal fun FormDialog(
    title: String,
    fields: List<Pair<String, String>>,
    close: () -> Unit,
    save: (List<String>) -> Boolean,
) {
    val values = remember { fields.map { mutableStateOf(it.second) } }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(title) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                fields.forEachIndexed { index, field ->
                    OutlinedTextField(
                        values[index].value,
                        { values[index].value = it },
                        label = { Text(field.first) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (error)
                    Text(
                        stringResource(R.string.check_fields),
                        color = MaterialTheme.colorScheme.error,
                    )
            }
        },
        confirmButton = {
            Button(onClick = { error = !save(values.map { it.value }) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
internal fun NamedDialog(
    type: String,
    item: NamedItem?,
    enabled: Boolean,
    close: () -> Unit,
    save: (JSONObject) -> Unit,
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var active by remember { mutableStateOf(item?.active ?: true) }
    var position by remember { mutableStateOf((item?.position ?: 0).toString()) }
    AlertDialog(
        onDismissRequest = close,
        title = {
            Text(stringResource(if (type == "locations") R.string.locations else R.string.payment))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    name,
                    { name = it },
                    label = { Text(stringResource(R.string.name)) },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(active, { active = it })
                    Text(stringResource(R.string.active), Modifier.padding(start = 8.dp))
                }
                if (type != "locations")
                    OutlinedTextField(
                        position,
                        { position = it },
                        label = { Text(stringResource(R.string.position)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val payload = JSONObject().put("name", name.trim()).put("active", active)
                    if (type != "locations") payload.put("position", position.toIntOrNull() ?: 0)
                    save(payload)
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
internal fun MovementDialog(
    variant: Variant,
    locations: List<NamedItem>,
    enabled: Boolean,
    close: () -> Unit,
    save: (JSONObject) -> Unit,
) {
    var kind by remember { mutableStateOf("entry") }
    var quantity by remember { mutableStateOf("1") }
    var note by remember { mutableStateOf("") }
    var from by remember { mutableStateOf<Int?>(null) }
    var to by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(stringResource(R.string.move_variant, variant.sku)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                        "entry" to stringResource(R.string.entry),
                        "adjustment" to stringResource(R.string.adjustment),
                        "transfer" to stringResource(R.string.transfer),
                    )
                    .forEach { (value, name) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(kind == value, { kind = value })
                            Text(name)
                        }
                    }
                OutlinedTextField(
                    quantity,
                    { quantity = it },
                    label = { Text(stringResource(R.string.quantity)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Choice(
                    stringResource(R.string.source_location),
                    from,
                    locations.filter { it.active },
                    stringResource(R.string.unassigned),
                    { from = it },
                )
                if (kind == "transfer")
                    Choice(
                        stringResource(R.string.destination),
                        to,
                        locations.filter { it.active },
                        stringResource(R.string.unassigned),
                        { to = it },
                    )
                OutlinedTextField(
                    note,
                    { note = it },
                    label = { Text(stringResource(R.string.reason)) },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    save(
                        JSONObject()
                            .put("variant_id", variant.id)
                            .put("kind", kind)
                            .put("quantity", quantity.toInt())
                            .put("location_id", from ?: JSONObject.NULL)
                            .put("destination_location_id", to ?: JSONObject.NULL)
                            .put("note", note)
                    )
                },
                enabled =
                    enabled &&
                        quantity.toIntOrNull()?.let {
                            it != 0 && (kind == "adjustment" || it > 0)
                        } == true &&
                        note.isNotBlank(),
            ) {
                Text(stringResource(R.string.register))
            }
        },
        dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
internal fun LabelDialog(title: String, detail: String, barcode: String, close: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("label-format", 0) }
    var width by remember { mutableStateOf(prefs.getInt("width", 60).toString()) }
    var height by remember { mutableStateOf(prefs.getInt("height", 40).toString()) }
    val w = width.toIntOrNull()
    val h = height.toIntOrNull()
    val bitmap =
        remember(w, h) {
            if (w != null && h != null && w in 40..100 && h in 25..100)
                Labels.bitmap(title, detail, barcode, w, h)
            else null
        }
    fun persist() {
        prefs.edit().putInt("width", w ?: 60).putInt("height", h ?: 40).apply()
    }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(stringResource(R.string.label)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        width,
                        { width = it },
                        label = { Text(stringResource(R.string.label_width)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        height,
                        { height = it },
                        label = { Text(stringResource(R.string.label_height)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                bitmap?.let {
                    Image(
                        it.asImageBitmap(),
                        stringResource(R.string.label_preview),
                        Modifier.fillMaxWidth().height(150.dp),
                    )
                }
                Text(
                    stringResource(R.string.label_help),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(
                    onClick = {
                        bitmap?.let {
                            persist()
                            Labels.share(context, it)
                        }
                    },
                    enabled = bitmap != null,
                ) {
                    Text(stringResource(R.string.share_png))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    bitmap?.let {
                        persist()
                        Labels.print(context, it, w!!, h!!)
                    }
                },
                enabled = bitmap != null,
            ) {
                Text(stringResource(R.string.print_pdf))
            }
        },
        dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.close)) } },
    )
}
