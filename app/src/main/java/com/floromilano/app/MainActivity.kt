package com.floromilano.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme =
                    lightColorScheme(
                        primary = Color(0xFF795B28),
                        onPrimary = Color.White,
                        background = Color(0xFFFAF8F4),
                        surface = Color(0xFFFAF8F4),
                        secondaryContainer = Color(0xFFEFE4CF),
                    )
            ) {
                StoreScreen()
            }
        }
    }
}

private fun money(cents: Long, currency: String): String =
    NumberFormat.getCurrencyInstance(java.util.Locale.ITALY)
        .apply { this.currency = Currency.getInstance(currency) }
        .format(BigDecimal.valueOf(cents, 2))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(vm: StoreViewModel = viewModel()) {
    val s = vm.state
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var location by remember { mutableStateOf<Int?>(null) }
    var method by remember { mutableStateOf<Int?>(null) }
    var confirmation by remember { mutableStateOf(false) }
    var movement by remember { mutableStateOf<Variant?>(null) }
    var label by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var editVariant by remember { mutableStateOf<Variant?>(null) }
    var productEditor by remember { mutableStateOf<Variant?>(null) }
    var createProduct by remember { mutableStateOf(false) }
    var addVariant by remember { mutableStateOf<Variant?>(null) }
    var namedEditor by remember { mutableStateOf<Pair<String, NamedItem?>?>(null) }
    var returnEditor by remember { mutableStateOf<JSONObject?>(null) }
    val canWrite = s.configured && s.online && !s.busy && !s.pending
    val total = s.cart.sumOf { it.variant.price * it.quantity }
    LaunchedEffect(s.url) {
        search = ""
        location = null
        method = null
        confirmation = false
        movement = null
        label = null
        editVariant = null
        productEditor = null
        createProduct = false
        addVariant = null
        namedEditor = null
        returnEditor = null
    }
    fun scan() {
        GmsBarcodeScanning.getClient(context)
            .startScan()
            .addOnSuccessListener { code ->
                val value = code.rawValue ?: return@addOnSuccessListener
                val place = s.locations.find { it.barcode == value && it.active }
                if (place != null) {
                    location = place.id
                    Toast.makeText(context, "Posizione: ${place.name}", Toast.LENGTH_LONG).show()
                } else {
                    search = value
                    vm.search(value)
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                        context,
                        "Scanner non disponibile. Verifica Google Play Services e riprova; puoi cercare il codice manualmente.",
                        Toast.LENGTH_LONG,
                    )
                    .show()
            }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Image(painterResource(R.drawable.ic_floro), null, Modifier.size(38.dp))
                        Column {
                            Text(s.storeName, style = MaterialTheme.typography.titleMedium)
                            Text("Gestione negozio", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                actions = {
                    TextButton(onClick = vm::refresh, enabled = s.configured && !s.busy) {
                        Text("Aggiorna")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                listOf("Vendi", "Inventario", "Magazzino", "Impostazioni").forEachIndexed {
                    index,
                    name ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = {
                            tab = index
                            if (index == 2) vm.refresh()
                        },
                        icon = { Text(listOf("€", "▦", "▤", "⚙")[index]) },
                        label = { Text(name, maxLines = 1) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (s.configured && !s.online)
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        "Connessione al server assente. Vendite e modifiche bloccate.",
                        Modifier.fillMaxWidth().padding(12.dp),
                        fontWeight = FontWeight.Bold,
                    )
                }
            if (s.pending)
                Surface(color = Color(0xFFFFE9B1)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(
                            "Una richiesta è in attesa di conferma. Non registrare di nuovo la stessa vendita."
                        )
                        Button(onClick = vm::retryPending, enabled = s.online && !s.busy) {
                            Text("Verifica / riprova richiesta")
                        }
                    }
                }
            if (s.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (s.message.isNotBlank())
                Text(
                    s.message,
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            if (!s.configured || tab == 3) {
                SettingsScreen(s, vm::configure)
            } else
                when (tab) {
                    0,
                    1 ->
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item {
                                OutlinedTextField(
                                    search,
                                    { search = it },
                                    label = { Text("Nome, SKU o codice") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { vm.search(search) }, enabled = !s.busy) {
                                        Text("Cerca")
                                    }
                                    OutlinedButton(onClick = ::scan, enabled = !s.busy) {
                                        Text("Scansiona")
                                    }
                                    if (tab == 1)
                                        TextButton(
                                            onClick = { createProduct = true },
                                            enabled = canWrite,
                                        ) {
                                            Text("Nuovo")
                                        }
                                }
                            }
                            if (tab == 0) {
                                item {
                                    Choice(
                                        "Preleva da",
                                        location,
                                        s.locations.filter { it.active },
                                        "Automatico",
                                        { location = it },
                                    )
                                }
                                if (s.cart.isNotEmpty())
                                    item {
                                        Card {
                                            Column(
                                                Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Text(
                                                    "Vendita in corso",
                                                    style = MaterialTheme.typography.titleMedium,
                                                )
                                                s.cart.forEachIndexed { index, line ->
                                                    Row(
                                                        Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                            Arrangement.SpaceBetween,
                                                    ) {
                                                        Column(Modifier.weight(1f)) {
                                                            Text(
                                                                "${line.quantity} × ${line.variant.name}"
                                                            )
                                                            Text(
                                                                line.variant.attributes,
                                                                style =
                                                                    MaterialTheme.typography
                                                                        .bodySmall,
                                                            )
                                                            Text(
                                                                s.locations
                                                                    .find {
                                                                        it.id == line.locationId
                                                                    }
                                                                    ?.name ?: "Prelievo automatico",
                                                                style =
                                                                    MaterialTheme.typography
                                                                        .bodySmall,
                                                            )
                                                        }
                                                        TextButton(
                                                            onClick = { vm.remove(index) },
                                                            enabled = !s.busy && !s.pending,
                                                        ) {
                                                            Text("Rimuovi")
                                                        }
                                                    }
                                                }
                                                Text(
                                                    "Totale: ${money(total, s.currency)}",
                                                    fontWeight = FontWeight.Bold,
                                                )
                                                Choice(
                                                    "Pagamento",
                                                    method,
                                                    s.methods.filter { it.active },
                                                    "Seleziona",
                                                    { method = it },
                                                )
                                                Button(
                                                    onClick = { confirmation = true },
                                                    enabled =
                                                        canWrite &&
                                                            s.methods.any {
                                                                it.id == method && it.active
                                                            },
                                                    modifier = Modifier.fillMaxWidth(),
                                                ) {
                                                    Text("Registra vendita")
                                                }
                                                Text(
                                                    "Registrazione interna. Nessun addebito automatico o documento fiscale.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                            }
                                        }
                                    }
                            }
                            items(s.variants, key = { it.id }) { variant ->
                                OutlinedCard(Modifier.fillMaxWidth()) {
                                    Column(
                                        Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            variant.name,
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Text(
                                            variant.attributes,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Text(
                                            "${variant.sku} · ${variant.barcode}",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                        Text(
                                            "${money(variant.price, s.currency)} · ${variant.available} disponibili / ${variant.stock} totali"
                                        )
                                        if (variant.stock <= variant.lowStock)
                                            Text(
                                                "Scorte basse",
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        if (tab == 0)
                                            Button(
                                                onClick = { vm.add(variant, location) },
                                                enabled =
                                                    canWrite &&
                                                        variant.available > 0 &&
                                                        variant.status != "archived",
                                            ) {
                                                Text("Aggiungi")
                                            }
                                        else {
                                            Text(
                                                "Non assegnate: ${variant.unassigned}",
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                            variant.locations.forEach { (id, quantity) ->
                                                Text(
                                                    "${s.locations.find { it.id == id }?.name ?: "Posizione #$id"}: $quantity",
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                            }
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                TextButton(
                                                    onClick = { movement = variant },
                                                    enabled = canWrite,
                                                ) {
                                                    Text("Movimento")
                                                }
                                                TextButton(
                                                    onClick = {
                                                        label =
                                                            Triple(
                                                                variant.name,
                                                                variant.attributes,
                                                                variant.barcode,
                                                            )
                                                    }
                                                ) {
                                                    Text("Etichetta")
                                                }
                                                TextButton(
                                                    onClick = { editVariant = variant },
                                                    enabled = canWrite,
                                                ) {
                                                    Text("Modifica")
                                                }
                                            }
                                            TextButton(
                                                onClick = { productEditor = variant },
                                                enabled = canWrite,
                                            ) {
                                                Text("Dati del prodotto")
                                            }
                                            TextButton(
                                                onClick = { addVariant = variant },
                                                enabled = canWrite,
                                            ) {
                                                Text("Nuova variante")
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    TextButton(
                                        onClick = { vm.search(s.query, s.page - 1) },
                                        enabled = !s.busy && s.page > 1,
                                    ) {
                                        Text("Precedente")
                                    }
                                    Text("${s.page} / ${s.lastPage}", Modifier.padding(12.dp))
                                    TextButton(
                                        onClick = { vm.search(s.query, s.page + 1) },
                                        enabled = !s.busy && s.page < s.lastPage,
                                    ) {
                                        Text("Successiva")
                                    }
                                }
                            }
                        }
                    2 ->
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item {
                                Text("Oggi", style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    "${s.metrics.optInt("today_sales")} vendite · ${money(s.metrics.optLong("today_total_cents"), s.currency)} (lordo)"
                                )
                                Text(
                                    "${s.metrics.optInt("units")} unità · ${s.metrics.optInt("low_stock_variants")} varianti con scorte basse"
                                )
                            }
                            item {
                                Text("Posizioni", style = MaterialTheme.typography.titleLarge)
                                TextButton(
                                    onClick = { namedEditor = "locations" to null },
                                    enabled = canWrite,
                                ) {
                                    Text("Nuova posizione")
                                }
                            }
                            items(s.locations) { place ->
                                ListItem(
                                    headlineContent = {
                                        Text(place.name + if (place.active) "" else " · inattiva")
                                    },
                                    supportingContent = { Text(place.barcode) },
                                    trailingContent = {
                                        Row {
                                            TextButton(
                                                onClick = {
                                                    label =
                                                        Triple(
                                                            place.name,
                                                            "Posizione",
                                                            place.barcode,
                                                        )
                                                }
                                            ) {
                                                Text("Etichetta")
                                            }
                                            TextButton(
                                                onClick = { namedEditor = "locations" to place },
                                                enabled = canWrite,
                                            ) {
                                                Text("Modifica")
                                            }
                                        }
                                    },
                                )
                            }
                            item {
                                Text("Pagamenti", style = MaterialTheme.typography.titleLarge)
                                TextButton(
                                    onClick = { namedEditor = "payment-methods" to null },
                                    enabled = canWrite,
                                ) {
                                    Text("Nuovo metodo")
                                }
                            }
                            items(s.methods) { payment ->
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            payment.name + if (payment.active) "" else " · inattivo"
                                        )
                                    },
                                    trailingContent = {
                                        TextButton(
                                            onClick = {
                                                namedEditor = "payment-methods" to payment
                                            },
                                            enabled = canWrite,
                                        ) {
                                            Text("Modifica")
                                        }
                                    },
                                )
                            }
                            item {
                                Text("Vendite recenti", style = MaterialTheme.typography.titleLarge)
                            }
                            val sales = s.history.optJSONArray("sales") ?: JSONArray()
                            items(sales.length()) { index ->
                                val sale = sales.getJSONObject(index)
                                OutlinedCard {
                                    Column(Modifier.padding(16.dp)) {
                                        Text(
                                            "#${sale.getInt("id")} · ${money(sale.getLong("total_cents"), sale.getString("currency"))}",
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            sale.getString("payment_method_name") +
                                                " · " +
                                                sale.getString("created_at").take(16)
                                        )
                                        val lines = sale.getJSONArray("lines")
                                        for (i in 0 until lines.length()) {
                                            val line = lines.getJSONObject(i)
                                            Text(
                                                "${line.getInt("quantity")} × ${line.getString("product_name")} · ${line.getString("sku")}"
                                            )
                                            TextButton(
                                                onClick = { returnEditor = line },
                                                enabled =
                                                    canWrite &&
                                                        line.getInt("returned_quantity") <
                                                            line.getInt("quantity"),
                                            ) {
                                                Text("Registra reso")
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                Text(
                                    "Ultimi movimenti",
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                            val movements = s.history.optJSONArray("movements") ?: JSONArray()
                            items(movements.length()) { index ->
                                val m = movements.getJSONObject(index)
                                Text(
                                    "#${m.getInt("id")} · variante ${m.getInt("product_variant_id")} · ${m.getString("kind")} · ${m.getInt("quantity")}\n${m.optString("note")}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                }
        }
    }
    if (confirmation)
        AlertDialog(
            onDismissRequest = { confirmation = false },
            title = { Text("Confermare la vendita?") },
            text = {
                Text(
                    "${money(total, s.currency)} · ${s.methods.find { it.id == method }?.name}\nIl server controllerà prezzo e disponibilità prima di registrarla."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmation = false
                        method?.let(vm::checkout)
                    },
                    enabled = canWrite,
                ) {
                    Text("Conferma")
                }
            },
            dismissButton = { TextButton(onClick = { confirmation = false }) { Text("Indietro") } },
        )
    movement?.let { variant ->
        MovementDialog(variant, s.locations, canWrite, { movement = null }) { payload ->
            vm.operate("/movements", payload)
            movement = null
        }
    }
    label?.let { (title, detail, barcode) -> LabelDialog(title, detail, barcode) { label = null } }
    namedEditor?.let { (type, item) ->
        NamedDialog(type, item, canWrite, { namedEditor = null }) { payload ->
            vm.saveResource("/$type" + (item?.let { "/${it.id}" } ?: ""), payload, item != null)
            namedEditor = null
        }
    }
    editVariant?.let { variant ->
        FormDialog(
            "Modifica variante",
            listOf(
                "SKU" to variant.sku,
                "Prezzo (€)" to BigDecimal.valueOf(variant.price, 2).toPlainString(),
                "Soglia scorte basse" to variant.lowStock.toString(),
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
        FormDialog(
            "Prodotto · dati condivisi tra varianti",
            listOf(
                "Nome" to variant.name,
                "Descrizione" to "",
                "Stato: draft / published / archived" to variant.status,
            ),
            { productEditor = null },
        ) { values ->
            if (
                !canWrite ||
                    values[0].isBlank() ||
                    values[2] !in listOf("draft", "published", "archived")
            )
                false
            else {
                val payload = JSONObject().put("name", values[0]).put("status", values[2])
                if (values[1].isNotBlank()) payload.put("description", values[1])
                vm.saveResource("/products/${variant.productId}", payload, true)
                productEditor = null
                true
            }
        }
    }
    if (createProduct || addVariant != null)
        ProductDialog(
            addVariant?.name,
            canWrite,
            {
                createProduct = false
                addVariant = null
            },
        ) { payload ->
            val path = addVariant?.let { "/products/${it.productId}/variants" } ?: "/products"
            vm.saveResource(path, payload)
            createProduct = false
            addVariant = null
        }
    returnEditor?.let { line ->
        FormDialog(
            "Reso · rimborso da effettuare separatamente",
            listOf("Quantità" to "1", "Motivo" to ""),
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

@Composable
private fun SettingsScreen(s: UiState, save: (String, String) -> Unit) {
    var url by remember(s.url) { mutableStateOf(s.url) }
    var key by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Collega il tuo negozio", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Usa l’indirizzo del negozio e una chiave creata in Amministrazione → Magazzino e app."
        )
        OutlinedTextField(
            url,
            { url = it },
            label = { Text("URL del negozio (https://…)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            key,
            { key = it },
            label = {
                Text(
                    if (s.configured) "Nuova chiave (vuota: conserva attuale)"
                    else "Chiave del dispositivo"
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
        Text(
            "Prima della vendita controlla l’indirizzo: staging è solo per le prove. Cambiare negozio svuota la vendita in corso. La chiave è conservata cifrata sul dispositivo.",
            style = MaterialTheme.typography.bodySmall,
        )
        Button(onClick = { confirm = true }, enabled = !s.busy) {
            Text("Verifica e salva connessione")
        }
        Text(
            "Connessione obbligatoria. Nessuna vendita viene accodata offline.",
            fontWeight = FontWeight.Medium,
        )
    }
    if (confirm)
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Collegare questo negozio?") },
            text = { Text(url) },
            confirmButton = {
                Button(
                    onClick = {
                        confirm = false
                        save(url, key)
                        key = ""
                    }
                ) {
                    Text("Collega")
                }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Annulla") } },
        )
}

@Composable
private fun Choice(
    label: String,
    selected: Int?,
    options: List<NamedItem>,
    empty: String,
    onSelect: (Int?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text("$label: ${options.find { it.id == selected }?.name ?: empty}")
        }
        DropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem(
                text = { Text(empty) },
                onClick = {
                    expanded = false
                    onSelect(null)
                },
            )
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.name) },
                    onClick = {
                        expanded = false
                        onSelect(item.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun FormDialog(
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
                        "Controlla i dati e la connessione al server.",
                        color = MaterialTheme.colorScheme.error,
                    )
            }
        },
        confirmButton = {
            Button(onClick = { error = !save(values.map { it.value }) }) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = close) { Text("Annulla") } },
    )
}

@Composable
private fun NamedDialog(
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
        title = { Text(if (type == "locations") "Posizione" else "Metodo di pagamento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nome") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(active, { active = it })
                    Text("Attivo", Modifier.padding(start = 8.dp))
                }
                if (type != "locations")
                    OutlinedTextField(
                        position,
                        { position = it },
                        label = { Text("Ordine") },
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
                Text("Salva")
            }
        },
        dismissButton = { TextButton(onClick = close) { Text("Annulla") } },
    )
}

@Composable
private fun MovementDialog(
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
        title = { Text("Movimento · ${variant.sku}") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                        "entry" to "Entrata",
                        "adjustment" to "Rettifica (+ / −)",
                        "transfer" to "Trasferimento",
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
                    label = { Text("Quantità") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Choice(
                    "Posizione / origine",
                    from,
                    locations.filter { it.active },
                    "Non assegnata",
                    { from = it },
                )
                if (kind == "transfer")
                    Choice(
                        "Destinazione",
                        to,
                        locations.filter { it.active },
                        "Non assegnata",
                        { to = it },
                    )
                OutlinedTextField(note, { note = it }, label = { Text("Motivo") })
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
                Text("Registra")
            }
        },
        dismissButton = { TextButton(onClick = close) { Text("Annulla") } },
    )
}

@Composable
private fun LabelDialog(title: String, detail: String, barcode: String, close: () -> Unit) {
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
        title = { Text("Etichetta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        width,
                        { width = it },
                        label = { Text("Larghezza mm") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        height,
                        { height = it },
                        label = { Text("Altezza mm") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                bitmap?.let {
                    Image(
                        it.asImageBitmap(),
                        "Anteprima etichetta",
                        Modifier.fillMaxWidth().height(150.dp),
                    )
                }
                Text(
                    "Stampa con un servizio Android compatibile oppure condividi il PNG. La compatibilità diretta con Katasymbol e Brother va verificata.",
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
                    Text("Condividi PNG")
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
                Text("Stampa / PDF")
            }
        },
        dismissButton = { TextButton(onClick = close) { Text("Chiudi") } },
    )
}
