package com.floromilano.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun CatalogScreen(
    s: UiState,
    query: String,
    onQuery: (String) -> Unit,
    onSearchNow: () -> Unit,
    onScan: () -> Unit,
    onBrowse: (Int?, Boolean, Boolean, String) -> Unit,
    onOpen: (CatalogProduct) -> Unit,
    onQuickAdd: (Variant) -> Unit,
    onMore: () -> Unit,
    onNew: () -> Unit,
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val preferences = remember { context.getSharedPreferences("appearance", 0) }
    var listView by rememberSaveable { mutableStateOf(preferences.getBoolean("list_view", false)) }
    var sortMenu by remember { mutableStateOf(false) }
    val grid = rememberLazyGridState()
    val inventory = s.catalogMode == "inventory"
    val canWrite = s.online && !s.busy && !s.pending
    LaunchedEffect(s.query, s.categoryId, s.inStockOnly, s.lowStockOnly, s.sort, s.catalogMode) {
        grid.scrollToItem(0)
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                query,
                onQuery,
                Modifier.weight(1f).testTag("catalog-search"),
                placeholder = {
                    Text(
                        stringResource(R.string.search_products),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_search), null, Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (query.isNotEmpty())
                        IconButton(onClick = { onQuery("") }) {
                            Icon(
                                painterResource(R.drawable.ic_close),
                                stringResource(R.string.clear_search),
                            )
                        }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions =
                    KeyboardActions(
                        onSearch = {
                            keyboard?.hide()
                            onSearchNow()
                        }
                    ),
            )
            Spacer(Modifier.width(8.dp))
            FilledTonalIconButton(
                onClick = {
                    keyboard?.hide()
                    onScan()
                },
                modifier = Modifier.size(52.dp),
                enabled = !s.busy,
            ) {
                Icon(painterResource(R.drawable.ic_scan), stringResource(R.string.scan))
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = s.categoryId == null,
                    onClick = { onBrowse(null, s.inStockOnly, s.lowStockOnly, s.sort) },
                    label = { Text(stringResource(R.string.all_categories)) },
                    modifier = Modifier.testTag("category-all"),
                )
            }
            items(
                s.categories.filter {
                    (if (s.catalogMode == "sale") it.saleProductCount else it.productCount) > 0 ||
                        it.id == s.categoryId
                },
                key = { it.id },
            ) { category ->
                val parent = s.categories.find { it.id == category.parentId }
                FilterChip(
                    selected = s.categoryId == category.id,
                    onClick = { onBrowse(category.id, s.inStockOnly, s.lowStockOnly, s.sort) },
                    label = {
                        Text(
                            if (parent == null) category.name
                            else "${parent.name} · ${category.name}"
                        )
                    },
                    modifier = Modifier.testTag("category-${category.id}"),
                )
            }
            item {
                FilterChip(
                    selected = s.categoryId == -1,
                    onClick = { onBrowse(-1, s.inStockOnly, s.lowStockOnly, s.sort) },
                    label = { Text(stringResource(R.string.uncategorized)) },
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.products_count, s.totalProducts),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (inventory) {
                IconButton(onClick = onNew, enabled = canWrite) {
                    Icon(painterResource(R.drawable.ic_plus), stringResource(R.string.new_product))
                }
            }
            Box {
                IconButton(onClick = { sortMenu = true }) {
                    Icon(painterResource(R.drawable.ic_sort), stringResource(R.string.sort))
                }
                DropdownMenu(sortMenu, { sortMenu = false }) {
                    listOf(
                            "name" to R.string.sort_name,
                            "price_asc" to R.string.sort_price_asc,
                            "price_desc" to R.string.sort_price_desc,
                        )
                        .forEach { (sort, label) ->
                            DropdownMenuItem(
                                text = { Text(stringResource(label)) },
                                onClick = {
                                    sortMenu = false
                                    onBrowse(s.categoryId, s.inStockOnly, s.lowStockOnly, sort)
                                },
                            )
                        }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (s.inStockOnly) R.string.all_stock
                                    else R.string.available_only
                                )
                            )
                        },
                        onClick = {
                            sortMenu = false
                            onBrowse(s.categoryId, !s.inStockOnly, false, s.sort)
                        },
                    )
                    if (inventory)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (s.lowStockOnly) R.string.all_stock
                                        else R.string.low_stock_only
                                    )
                                )
                            },
                            onClick = {
                                sortMenu = false
                                onBrowse(s.categoryId, false, !s.lowStockOnly, s.sort)
                            },
                        )
                }
            }
            IconButton(
                onClick = {
                    listView = !listView
                    preferences.edit().putBoolean("list_view", listView).apply()
                }
            ) {
                Icon(
                    painterResource(
                        if (listView) R.drawable.ic_inventory else R.drawable.ic_warehouse
                    ),
                    stringResource(if (listView) R.string.grid_view else R.string.list_view),
                )
            }
            if (inventory)
                IconButton(onClick = onNew, enabled = canWrite) {
                    Icon(painterResource(R.drawable.ic_plus), stringResource(R.string.new_product))
                }
        }
        if (s.inStockOnly || s.lowStockOnly) {
            InputChip(
                selected = true,
                onClick = { onBrowse(s.categoryId, false, false, s.sort) },
                label = {
                    Text(
                        stringResource(
                            if (s.lowStockOnly) R.string.low_stock_only else R.string.available_only
                        )
                    )
                },
                trailingIcon = {
                    Icon(
                        painterResource(R.drawable.ic_close),
                        stringResource(R.string.reset_filters),
                        Modifier.size(16.dp),
                    )
                },
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        if (s.catalogLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (s.products.isEmpty() && !s.catalogLoading)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_search),
                        null,
                        Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        stringResource(R.string.empty_catalog),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.empty_catalog_hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(
                        onClick = {
                            onQuery("")
                            onBrowse(null, false, false, "name")
                        }
                    ) {
                        Text(stringResource(R.string.reset_filters))
                    }
                }
            }
        else
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (listView) 1 else 2),
                state = grid,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f).testTag("product-grid"),
            ) {
                items(s.products, key = { it.id }) { product ->
                    val quantity =
                        s.cart.filter { it.variant.productId == product.id }.sumOf { it.quantity }
                    ProductCard(
                        product,
                        quantity,
                        s.currency,
                        inventory,
                        listView,
                        canWrite,
                        { onOpen(product) },
                        {
                            if (product.variants.size == 1 && !inventory)
                                onQuickAdd(product.variants.single())
                            else onOpen(product)
                        },
                    )
                }
                if (s.page < s.lastPage)
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        OutlinedButton(
                            onClick = onMore,
                            enabled = !s.busy && !s.catalogLoading,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.load_more))
                        }
                    }
            }
    }
}

@Composable
private fun ProductCard(
    product: CatalogProduct,
    quantity: Int,
    currency: String,
    inventory: Boolean,
    compact: Boolean,
    canWrite: Boolean,
    onOpen: () -> Unit,
    add: () -> Unit,
) {
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().testTag("product-card-${product.id}"),
    ) {
        if (compact)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                ProductPhoto(product.imageUrl, Modifier.size(width = 64.dp, height = 80.dp))
                Column(
                    Modifier.weight(1f).padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        product.name,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(money(product.minimumPrice, currency), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.variant_count, product.variants.size) +
                            " · " +
                            stringResource(R.string.available_count, product.available),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                CardAdd(inventory, canWrite && product.available > 0, quantity, add)
            }
        else
            Column {
                Box {
                    ProductPhoto(product.imageUrl, Modifier.fillMaxWidth().height(110.dp))
                    if (quantity > 0)
                        Badge(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                            Text(quantity.toString())
                        }
                    if (inventory && product.lowStock)
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(5.dp),
                            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                        ) {
                            Text(
                                stringResource(R.string.low_stock),
                                Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                }
                Column(Modifier.padding(start = 10.dp, end = 6.dp, top = 8.dp)) {
                    Text(
                        product.name,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        stringResource(R.string.variant_count, product.variants.size) +
                            " · " +
                            stringResource(R.string.available_count, product.available),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (product.minimumPrice != product.maximumPrice)
                                stringResource(
                                    R.string.price_from,
                                    money(product.minimumPrice, currency),
                                )
                            else money(product.minimumPrice, currency),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        CardAdd(inventory, canWrite && product.available > 0, 0, add)
                    }
                }
            }
    }
}

@Composable
private fun CardAdd(inventory: Boolean, enabled: Boolean, quantity: Int, add: () -> Unit) {
    IconButton(onClick = add, enabled = inventory || enabled) {
        if (quantity > 0)
            Text(
                quantity.toString(),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        else
            Icon(
                painterResource(if (inventory) R.drawable.ic_arrow else R.drawable.ic_plus),
                stringResource(if (inventory) R.string.manage_variant else R.string.add),
                tint =
                    if (inventory || enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
            )
    }
}
