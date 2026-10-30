package com.floromilano.app

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.math.BigDecimal
import java.net.URI
import kotlinx.coroutines.delay
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private val storeViewModel by lazy { ViewModelProvider(this)[StoreViewModel::class.java] }

    override fun attachBaseContext(newBase: Context) =
        super.attachBaseContext(AppLanguage.wrap(newBase))

    override fun onStart() {
        super.onStart()
        storeViewModel.setForeground(true)
    }

    override fun onStop() {
        storeViewModel.setForeground(false)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FloroTheme { StoreScreen(storeViewModel) } }
    }
}

@Composable
internal fun FloroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme =
            lightColorScheme(
                primary = Color(0xFF795B28),
                onPrimary = Color.White,
                background = Color(0xFFF6F4F0),
                surface = Color.White,
                surfaceContainer = Color(0xFFF3EFE8),
                surfaceContainerLow = Color(0xFFF1EFEB),
                secondaryContainer = Color(0xFFF0E5D1),
                onSecondaryContainer = Color(0xFF48381D),
            ),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(vm: StoreViewModel = viewModel()) {
    val s = vm.state
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf(s.query) }
    var previousOrigin by rememberSaveable { mutableStateOf(s.url) }
    var scanQuery by remember { mutableStateOf<String?>(null) }
    var location by rememberSaveable { mutableStateOf<Int?>(null) }
    var method by rememberSaveable { mutableStateOf<Int?>(null) }
    var product by remember { mutableStateOf<CatalogProduct?>(null) }
    var scannedVariant by remember { mutableStateOf<Int?>(null) }
    var cartOpen by rememberSaveable { mutableStateOf(false) }
    var movement by remember { mutableStateOf<Variant?>(null) }
    var label by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var editVariant by remember { mutableStateOf<Variant?>(null) }
    var productEditor by remember { mutableStateOf<Variant?>(null) }
    var createProduct by remember { mutableStateOf(false) }
    var addVariant by remember { mutableStateOf<Variant?>(null) }
    var namedEditor by remember { mutableStateOf<Pair<String, NamedItem?>?>(null) }
    var returnEditor by remember { mutableStateOf<JSONObject?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val canWrite = s.configured && s.online && !s.busy && !s.pending
    val total = s.cart.sumOf { it.variant.price * it.quantity }
    val cartCount = s.cart.sumOf { it.quantity }
    LaunchedEffect(s.url) {
        if (s.url != previousOrigin) {
            query = ""
            location = null
            method = null
            product = null
            cartOpen = false
            scanQuery = null
            previousOrigin = s.url
        }
    }
    LaunchedEffect(s.cart.isEmpty()) { if (s.cart.isEmpty()) cartOpen = false }
    LaunchedEffect(s.message) { if (s.message.isNotBlank()) snackbar.showSnackbar(s.message) }
    LaunchedEffect(query, tab, s.busy, s.configured) {
        if (s.configured && !s.busy && tab < 2) {
            delay(300)
            val mode = if (tab == 1) "inventory" else "sale"
            if (query != vm.state.query || mode != vm.state.catalogMode)
                vm.browse(query = query, mode = mode)
        }
    }
    LaunchedEffect(scanQuery) {
        val code = scanQuery ?: return@LaunchedEffect
        while (vm.state.busy) delay(100)
        vm.browse(
            query = code,
            categoryId = null,
            inStock = false,
            lowStock = false,
            mode = if (tab == 1) "inventory" else "sale",
        )
        while (vm.state.catalogLoading) delay(100)
        val match = vm.state.variants.find { it.barcode == code || it.sku == code }
        if (match != null && tab == 0) vm.add(match, location)
        else if (match != null) {
            scannedVariant = match.id
            product = vm.state.products.find { it.id == match.productId }
        }
        scanQuery = null
    }
    fun scan() {
        GmsBarcodeScanning.getClient(context)
            .startScan()
            .addOnSuccessListener { barcode ->
                val value = barcode.rawValue ?: return@addOnSuccessListener
                val place = vm.state.locations.find { it.barcode == value && it.active }
                if (place != null) {
                    location = place.id
                    Toast.makeText(
                            context,
                            context.getString(R.string.location_selected, place.name),
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                } else {
                    query = value.take(100)
                    scanQuery = value.take(100)
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                        context,
                        context.getString(R.string.scanner_unavailable),
                        Toast.LENGTH_LONG,
                    )
                    .show()
            }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.ic_floro), null, Modifier.size(34.dp))
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(
                                s.storeName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                if (s.configured)
                                    runCatching { URI(s.url).host }.getOrNull() ?: s.url
                                else stringResource(R.string.connect_store),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = vm::refresh, enabled = s.configured && !s.busy) {
                        Icon(
                            painterResource(R.drawable.ic_refresh),
                            stringResource(R.string.refresh),
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
            )
        },
        bottomBar = {
            Column {
                if (s.configured && tab == 0 && cartCount > 0) {
                    Button(
                        onClick = { cartOpen = true },
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .heightIn(min = 54.dp)
                                .testTag("view-cart"),
                    ) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.onPrimary,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ) {
                            Text(cartCount.toString())
                        }
                        Text(
                            stringResource(R.string.view_cart),
                            Modifier.weight(1f).padding(horizontal = 12.dp),
                        )
                        Text(money(total, s.currency), fontWeight = FontWeight.Bold)
                        Icon(painterResource(R.drawable.ic_arrow), null, Modifier.size(18.dp))
                    }
                }
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    listOf(
                            R.string.sell to R.drawable.ic_sell,
                            R.string.inventory to R.drawable.ic_inventory,
                            R.string.warehouse to R.drawable.ic_warehouse,
                            R.string.settings to R.drawable.ic_settings,
                        )
                        .forEachIndexed { index, (title, icon) ->
                            NavigationBarItem(
                                selected = tab == index,
                                onClick = {
                                    tab = index
                                    if (index == 2) vm.refresh()
                                },
                                icon = { Icon(painterResource(icon), null) },
                                label = {
                                    Text(
                                        stringResource(title),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                modifier = Modifier.testTag("nav-$index"),
                            )
                        }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (s.configured && !s.online)
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        stringResource(R.string.no_connection),
                        Modifier.fillMaxWidth().padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            if (s.pending)
                Surface(color = Color(0xFFFFEDC4)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(
                            stringResource(R.string.pending_notice),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(onClick = vm::retryPending, enabled = s.online && !s.busy) {
                            Text(stringResource(R.string.resolve_pending))
                        }
                    }
                }
            if (s.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (!s.configured || tab == 3)
                SettingsScreen(s, vm::configure) { language ->
                    AppLanguage.select(context, language)
                    vm.languageChanged()
                    (context as? MainActivity)?.recreate()
                }
            else if (tab == 2)
                WarehouseScreen(
                    s,
                    { type, item -> namedEditor = type to item },
                    { title, detail, code -> label = Triple(title, detail, code) },
                    { returnEditor = it },
                )
            else
                CatalogScreen(
                    s,
                    query,
                    { query = it.take(100) },
                    { vm.browse(query = query) },
                    ::scan,
                    { category, stock, low, sort ->
                        vm.browse(
                            query = query,
                            categoryId = category,
                            inStock = stock,
                            lowStock = low,
                            sort = sort,
                            mode = if (tab == 1) "inventory" else "sale",
                        )
                    },
                    {
                        scannedVariant = null
                        product = it
                    },
                    { vm.add(it, location) },
                    { vm.browse(page = s.page + 1) },
                    { createProduct = true },
                )
        }
    }
    product?.let { selected ->
        ProductSheet(
            selected,
            s,
            tab == 1,
            location,
            { location = it },
            { product = null },
            { variant, quantity ->
                vm.add(variant, location, quantity)
                product = null
            },
            { variant, action ->
                product = null
                when (action) {
                    "movement" -> movement = variant
                    "label" -> label = Triple(variant.name, variant.attributes, variant.barcode)
                    "edit" -> editVariant = variant
                }
            },
            {
                product = null
                productEditor = selected.variants.first()
            },
            {
                product = null
                addVariant = selected.variants.first()
            },
            scannedVariant,
        )
    }
    if (cartOpen && s.cart.isNotEmpty())
        CartSheet(
            s,
            method,
            { method = it },
            { cartOpen = false },
            vm::reduce,
            { index -> s.cart.getOrNull(index)?.let { vm.add(it.variant, it.locationId) } },
            vm::remove,
            {
                method?.let(vm::checkout)
                cartOpen = false
            },
        )
    movement?.let {
        MovementDialog(it, s.locations, canWrite, { movement = null }) { payload ->
            vm.operate("/movements", payload)
            movement = null
        }
    }
    label?.let { (title, detail, code) -> LabelDialog(title, detail, code) { label = null } }
    namedEditor?.let { (type, item) ->
        NamedDialog(type, item, canWrite, { namedEditor = null }) { payload ->
            vm.saveResource("/$type" + (item?.let { "/${it.id}" } ?: ""), payload, item != null)
            namedEditor = null
        }
    }
    editVariant?.let { variant ->
        FormDialog(
            stringResource(R.string.edit_variant),
            listOf(
                stringResource(R.string.sku) to variant.sku,
                stringResource(R.string.price) to
                    BigDecimal.valueOf(variant.price, 2).toPlainString().replace('.', ','),
                stringResource(R.string.low_stock_threshold) to variant.lowStock.toString(),
            ),
            { editVariant = null },
        ) { values ->
            val cents = priceCents(values[1])
            val threshold = values[2].toIntOrNull()
            if (
                !canWrite ||
                    cents == null ||
                    threshold == null ||
                    threshold < 0 ||
                    values[0].isBlank()
            )
                false
            else {
                vm.saveResource(
                    "/variants/${variant.id}",
                    JSONObject()
                        .put("sku", values[0])
                        .put("price_cents", cents)
                        .put("low_stock_threshold", threshold),
                    true,
                )
                editVariant = null
                true
            }
        }
    }
    productEditor?.let { variant ->
        ProductDataDialog(variant, s.categories, canWrite, { productEditor = null }) { payload ->
            vm.saveResource("/products/${variant.productId}", payload, true)
            productEditor = null
        }
    }
    if (createProduct || addVariant != null)
        ProductDialog(
            addVariant?.name,
            s.categories,
            canWrite,
            {
                createProduct = false
                addVariant = null
            },
        ) { payload ->
            vm.saveResource(
                addVariant?.let { "/products/${it.productId}/variants" } ?: "/products",
                payload,
            )
            createProduct = false
            addVariant = null
        }
    returnEditor?.let { line ->
        FormDialog(
            stringResource(R.string.return_notice),
            listOf(stringResource(R.string.quantity) to "1", stringResource(R.string.reason) to ""),
            { returnEditor = null },
        ) { values ->
            val quantity = values[0].toIntOrNull()
            if (!canWrite || quantity == null || quantity <= 0 || values[1].isBlank()) false
            else {
                vm.operate(
                    "/returns",
                    JSONObject()
                        .put("sale_line_id", line.getInt("id"))
                        .put("quantity", quantity)
                        .put("note", values[1]),
                )
                returnEditor = null
                true
            }
        }
    }
}
