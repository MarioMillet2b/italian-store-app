package com.floromilano.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

@Composable
internal fun WarehouseScreen(
    s: UiState,
    onNamed: (String, NamedItem?) -> Unit,
    onLabel: (String, String, String) -> Unit,
    onReturn: (JSONObject) -> Unit,
) {
    var section by remember { mutableIntStateOf(0) }
    var expandedSale by remember { mutableStateOf<Int?>(null) }
    val canWrite = s.online && !s.busy && !s.pending
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric(
                    stringResource(R.string.gross_sales),
                    money(s.metrics.optLong("today_total_cents"), s.currency),
                    Modifier.weight(1f),
                )
                Metric(
                    stringResource(R.string.today),
                    s.metrics.optInt("today_sales").toString(),
                    Modifier.weight(1f),
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric(
                    stringResource(R.string.units),
                    s.metrics.optInt("units").toString(),
                    Modifier.weight(1f),
                )
                Metric(
                    stringResource(R.string.alerts),
                    s.metrics.optInt("low_stock_variants").toString(),
                    Modifier.weight(1f),
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.locations, R.string.payment_methods, R.string.recent_sales)
                    .forEachIndexed { index, id ->
                        FilterChip(
                            section == index,
                            { section = index },
                            label = { Text(stringResource(id), maxLines = 2) },
                        )
                    }
            }
        }
        when (section) {
            0 -> {
                item {
                    Button(onClick = { onNamed("locations", null) }, enabled = canWrite) {
                        Text(stringResource(R.string.new_location))
                    }
                }
                if (s.locations.isEmpty()) item { Text(stringResource(R.string.no_locations)) }
                items(s.locations.size) { index ->
                    val location = s.locations[index]
                    OutlinedCard {
                        ListItem(
                            headlineContent = { Text(location.name) },
                            supportingContent = {
                                if (!location.active) Text(stringResource(R.string.inactive))
                            },
                            leadingContent = {
                                Icon(painterResource(R.drawable.ic_warehouse), null)
                            },
                            trailingContent = {
                                Row {
                                    IconButton(
                                        onClick = {
                                            onLabel(
                                                location.name,
                                                location.barcode,
                                                location.barcode,
                                            )
                                        }
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.ic_scan),
                                            stringResource(R.string.label),
                                        )
                                    }
                                    IconButton(
                                        onClick = { onNamed("locations", location) },
                                        enabled = canWrite,
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.ic_settings),
                                            stringResource(R.string.edit_location),
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
            1 -> {
                item {
                    Button(onClick = { onNamed("payment-methods", null) }, enabled = canWrite) {
                        Text(stringResource(R.string.new_payment))
                    }
                }
                items(s.methods.size) { index ->
                    val method = s.methods[index]
                    OutlinedCard {
                        ListItem(
                            headlineContent = { Text(method.name) },
                            supportingContent = {
                                Text(
                                    stringResource(
                                        if (method.active) R.string.active else R.string.inactive
                                    )
                                )
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = { onNamed("payment-methods", method) },
                                    enabled = canWrite,
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_settings),
                                        stringResource(R.string.payment),
                                    )
                                }
                            },
                        )
                    }
                }
            }
            else -> {
                val sales = s.history.optJSONArray("sales") ?: JSONArray()
                if (sales.length() == 0) item { Text(stringResource(R.string.no_sales)) }
                items(sales.length()) { index ->
                    val sale = sales.getJSONObject(index)
                    val id = sale.getInt("id")
                    OutlinedCard(
                        Modifier.fillMaxWidth().clickable {
                            expandedSale = if (expandedSale == id) null else id
                        }
                    ) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("#$id", fontWeight = FontWeight.Bold)
                                Text(
                                    money(sale.getLong("total_cents"), sale.getString("currency")),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Text(
                                sale.getString("payment_method_name") +
                                    " · " +
                                    localDate(sale.getString("created_at")),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (expandedSale == id) {
                                val lines = sale.getJSONArray("lines")
                                for (i in 0 until lines.length()) {
                                    val line = lines.getJSONObject(i)
                                    HorizontalDivider()
                                    Text(
                                        "${line.getInt("quantity")} × ${line.getString("product_name")}",
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        line.getString("sku"),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    TextButton(
                                        onClick = { onReturn(line) },
                                        enabled =
                                            canWrite &&
                                                line.getInt("returned_quantity") <
                                                    line.getInt("quantity"),
                                    ) {
                                        Text(stringResource(R.string.register_return))
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.recent_movements),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                val movements = s.history.optJSONArray("movements") ?: JSONArray()
                items(movements.length()) { index ->
                    val movement = movements.getJSONObject(index)
                    ListItem(
                        headlineContent = { Text(movementName(movement.getString("kind"))) },
                        supportingContent = {
                            Text(
                                stringResource(
                                    R.string.variant_id,
                                    movement.getInt("product_variant_id"),
                                ) + " · " + movement.optString("note", "")
                            )
                        },
                        trailingContent = { Text(movement.getInt("quantity").toString()) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun movementName(kind: String): String =
    stringResource(
        when {
            kind.endsWith("_allocation") -> R.string.movement_allocation
            kind == "pos_sale" -> R.string.movement_pos_sale
            kind == "online_sale" -> R.string.movement_online_sale
            kind.endsWith("return") -> R.string.movement_return
            kind == "transfer" -> R.string.transfer
            kind == "entry" -> R.string.entry
            else -> R.string.movement_adjustment
        }
    )

private fun localDate(value: String): String =
    runCatching {
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(Locale.getDefault())
                .withZone(ZoneId.systemDefault())
                .format(Instant.parse(value))
        }
        .getOrElse { value.take(16).replace('T', ' ') }
