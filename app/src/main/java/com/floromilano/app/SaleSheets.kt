package com.floromilano.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductSheet(
    product: CatalogProduct,
    s: UiState,
    inventory: Boolean,
    location: Int?,
    onLocation: (Int?) -> Unit,
    close: () -> Unit,
    add: (Variant, Int) -> Unit,
    onManage: (Variant, String) -> Unit,
    onProduct: () -> Unit,
    onNewVariant: () -> Unit,
    initialVariant: Int? = null,
) {
    val canWrite = s.online && !s.busy && !s.pending
    var selectedId by
        remember(product.id) {
            mutableStateOf(
                initialVariant
                    ?: product.variants.firstOrNull { it.available > 0 }?.id
                    ?: product.variants.first().id
            )
        }
    var quantity by remember { mutableIntStateOf(1) }
    val selected = product.variants.first { it.id == selectedId }
    val remaining = CartAvailability.remaining(selected, s.cart, location)
    LaunchedEffect(selected.id, remaining) { quantity = quantity.coerceAtMost(maxOf(1, remaining)) }
    ModalBottomSheet(
        onDismissRequest = close,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth()
                .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.88f).dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductPhoto(
                    selected.imageUrl ?: product.imageUrl,
                    Modifier.size(width = 64.dp, height = 80.dp),
                )
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(product.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        product.categoryName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        stringResource(
                            if (inventory) R.string.manage_variant else R.string.choose_variant
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                IconButton(onClick = close) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.close))
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(product.variants, key = { it.id }) { variant ->
                    val available = CartAvailability.remaining(variant, s.cart, location)
                    val enabled = inventory || available > 0
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color =
                            if (variant.id == selectedId)
                                MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable(enabled = enabled) {
                                    selectedId = variant.id
                                    quantity = 1
                                }
                                .testTag("variant-${variant.id}"),
                    ) {
                        Row(
                            Modifier.padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedId == variant.id,
                                onClick = {
                                    selectedId = variant.id
                                    quantity = 1
                                },
                                enabled = enabled,
                            )
                            Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                                Text(
                                    variant.attributes.ifBlank { variant.sku },
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    if (inventory) "${variant.sku} · ${variant.stock}"
                                    else if (available == 0) stringResource(R.string.sold_out)
                                    else
                                        pluralStringResource(
                                            R.plurals.available_count,
                                            available,
                                            available,
                                        ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(money(variant.price, s.currency), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (inventory) {
                Text(
                    stringResource(R.string.unassigned_count, selected.unassigned),
                    style = MaterialTheme.typography.bodySmall,
                )
                selected.locations.forEach { (id, units) ->
                    Text(
                        "${s.locations.find { it.id == id }?.name ?: "#$id"}: $units",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onManage(selected, "movement") },
                        enabled = canWrite,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.movement))
                    }
                    OutlinedButton(
                        onClick = { onManage(selected, "label") },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.label))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { onManage(selected, "edit") }, enabled = canWrite) {
                        Text(stringResource(R.string.edit_variant))
                    }
                    TextButton(onClick = onProduct, enabled = canWrite) {
                        Text(stringResource(R.string.product_data))
                    }
                }
                TextButton(onClick = onNewVariant, enabled = canWrite) {
                    Text(stringResource(R.string.new_variant))
                }
            } else {
                Choice(
                    stringResource(R.string.sale_location),
                    location,
                    s.locations.filter { it.active },
                    stringResource(R.string.automatic),
                    onLocation,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(R.string.quantity),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    QuantityControl(
                        quantity,
                        { quantity-- },
                        { quantity++ },
                        canWrite,
                        remaining > quantity,
                        quantity > 1,
                    )
                }
                Button(
                    onClick = { add(selected, quantity) },
                    enabled = canWrite && remaining >= quantity,
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("add-selected-variant"),
                ) {
                    Text(
                        stringResource(
                            R.string.add_quantity,
                            money(selected.price * quantity, s.currency),
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CartSheet(
    s: UiState,
    methodId: Int?,
    onMethod: (Int?) -> Unit,
    close: () -> Unit,
    onMinus: (Int) -> Unit,
    onPlus: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    submit: () -> Unit,
) {
    val enabled = s.online && !s.busy && !s.pending
    val total = s.cart.sumOf { it.variant.price * it.quantity }
    ModalBottomSheet(
        onDismissRequest = close,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth()
                .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.88f).dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.cart),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.cart_items, s.cart.sumOf { it.quantity }),
                    style = MaterialTheme.typography.labelLarge,
                )
                IconButton(onClick = close) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.close))
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 350.dp)) {
                items(s.cart.size, key = { "${s.cart[it].variant.id}:${s.cart[it].locationId}" }) {
                    index ->
                    val line = s.cart[index]
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProductPhoto(
                                line.variant.imageUrl,
                                Modifier.size(width = 44.dp, height = 56.dp),
                            )
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(
                                    line.variant.name,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    line.variant.attributes,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    s.locations.find { it.id == line.locationId }?.name
                                        ?: stringResource(R.string.automatic),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { onRemove(index) }, enabled = enabled) {
                                Icon(
                                    painterResource(R.drawable.ic_close),
                                    stringResource(R.string.remove),
                                    Modifier.size(18.dp),
                                )
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            QuantityControl(
                                line.quantity,
                                { onMinus(index) },
                                { onPlus(index) },
                                enabled,
                                CartAvailability.remaining(line.variant, s.cart, line.locationId) >
                                    0,
                            )
                            Text(
                                money(line.variant.price * line.quantity, s.currency),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
            Choice(
                stringResource(R.string.payment),
                methodId,
                s.methods.filter { it.active },
                stringResource(R.string.select_payment),
                onMethod,
            )
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.total), style = MaterialTheme.typography.titleLarge)
                Text(
                    money(total, s.currency),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Button(
                onClick = submit,
                enabled =
                    enabled &&
                        s.cart.isNotEmpty() &&
                        s.methods.any { it.id == methodId && it.active },
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("register-sale"),
            ) {
                Text(stringResource(R.string.checkout))
            }
            Text(
                stringResource(R.string.internal_sale_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
