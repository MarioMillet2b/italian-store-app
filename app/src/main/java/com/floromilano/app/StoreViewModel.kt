package com.floromilano.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.net.URLEncoder
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class Variant(
    val id: Int,
    val productId: Int,
    val name: String,
    val sku: String,
    val barcode: String,
    val attributes: String,
    val price: Long,
    val stock: Int,
    val available: Int,
    val unassigned: Int,
    val lowStock: Int,
    val locations: List<Pair<Int, Int>>,
    val status: String,
)

data class NamedItem(
    val id: Int,
    val name: String,
    val active: Boolean,
    val barcode: String = "",
    val position: Int = 0,
)

data class CartLine(val variant: Variant, val quantity: Int, val locationId: Int?)

data class UiState(
    val url: String = "",
    val configured: Boolean = false,
    val online: Boolean = false,
    val busy: Boolean = false,
    val message: String = "",
    val storeName: String = "Floro Milano",
    val variants: List<Variant> = emptyList(),
    val locations: List<NamedItem> = emptyList(),
    val methods: List<NamedItem> = emptyList(),
    val cart: List<CartLine> = emptyList(),
    val pending: Boolean = false,
    val page: Int = 1,
    val lastPage: Int = 1,
    val query: String = "",
    val metrics: JSONObject = JSONObject(),
    val history: JSONObject = JSONObject(),
    val currency: String = "EUR",
)

class StoreViewModel(application: Application) : AndroidViewModel(application) {
    private val vault = (application as StoreApplication).vault
    private var vaultFailed = false
    private var sessionVersion = 0
    private var saved =
        try {
            vault.read()
        } catch (_: Exception) {
            vaultFailed = true
            JSONObject()
        }
    var state by
        mutableStateOf(
            UiState(
                url = saved.optString("url"),
                configured = saved.optString("token").isNotBlank(),
                pending = saved.has("pending"),
                message =
                    if (vaultFailed)
                        "Archivio locale non leggibile. Verifica sul server eventuali vendite prima di ripristinare i dati dell’app."
                    else "",
            )
        )
        private set

    private fun api() = StoreApi(saved.getString("url"), saved.getString("token"))

    init {
        viewModelScope.launch {
            while (true) {
                if (state.configured && !state.busy) {
                    val version = sessionVersion
                    try {
                        loadContext()
                        if (state.variants.isEmpty()) loadVariants()
                    } catch (e: Exception) {
                        if (version == sessionVersion) connectionFailure(e)
                    }
                }
                delay(10000)
            }
        }
    }

    fun configure(url: String, token: String) {
        if (state.busy || vaultFailed) return
        state = state.copy(busy = true)
        viewModelScope.launch {
            try {
                val normalized = StoreUrl.normalize(url)
                require(!state.pending || normalized == saved.optString("url")) {
                    "Risolvi prima la richiesta in sospeso per cambiare negozio."
                }
                val credential =
                    token.trim().ifBlank {
                        if (normalized == saved.optString("url")) saved.optString("token") else ""
                    }
                require(credential.isNotBlank()) { "Inserisci la chiave del dispositivo." }
                val context = StoreApi(normalized, credential).request("/context")
                require(context.optInt("api_version") == 1) { "Versione API non compatibile." }
                val replacement = JSONObject().put("url", normalized).put("token", credential)
                if (saved.has("pending")) replacement.put("pending", saved.getJSONObject("pending"))
                vault.write(replacement)
                saved = replacement
                sessionVersion++
                state =
                    UiState(
                        url = normalized,
                        configured = true,
                        online = true,
                        busy = true,
                        pending = saved.has("pending"),
                        message = "Negozio collegato.",
                    )
                applyContext(context)
                loadVariants()
            } catch (e: Exception) {
                state = state.copy(message = e.message ?: "Connessione non riuscita.")
            } finally {
                state = state.copy(busy = false)
            }
        }
    }

    private fun named(array: JSONArray, code: Boolean = false): List<NamedItem> =
        (0 until array.length()).map {
            val x = array.getJSONObject(it)
            NamedItem(
                x.getInt("id"),
                x.getString("name"),
                x.optBoolean("active"),
                if (code) x.optString("barcode") else "",
                x.optInt("position"),
            )
        }

    private fun applyContext(json: JSONObject) {
        state =
            state.copy(
                online = true,
                storeName = json.getString("store_name"),
                locations = named(json.getJSONArray("locations"), true),
                methods = named(json.getJSONArray("payment_methods")),
                metrics = json.getJSONObject("metrics"),
                currency = json.getString("currency"),
            )
    }

    private suspend fun loadContext() {
        val version = sessionVersion
        val response = api().request("/context")
        if (version == sessionVersion) applyContext(response)
    }

    private suspend fun loadVariants() {
        val version = sessionVersion
        val query = URLEncoder.encode(state.query, "UTF-8")
        val response = api().request("/variants?q=$query&page=${state.page}")
        if (version != sessionVersion) return
        val array = response.getJSONArray("data")
        val variants =
            (0 until array.length()).map {
                val v = array.getJSONObject(it)
                val attrs = v.getJSONObject("attributes")
                val balances = v.getJSONArray("locations")
                Variant(
                    v.getInt("id"),
                    v.getInt("product_id"),
                    v.getString("name"),
                    v.getString("sku"),
                    v.getString("barcode"),
                    attrs.keys().asSequence().joinToString(" · ") { k ->
                        "$k: ${attrs.getString(k)}"
                    },
                    v.getLong("unit_price_cents"),
                    v.getInt("stock"),
                    v.getInt("available"),
                    v.getInt("unassigned"),
                    v.getInt("low_stock_threshold"),
                    (0 until balances.length()).map { b ->
                        balances.getJSONObject(b).let { x ->
                            x.getInt("inventory_location_id") to x.getInt("quantity")
                        }
                    },
                    v.getString("status"),
                )
            }
        state =
            state.copy(variants = variants, lastPage = response.getInt("last_page"), online = true)
    }

    fun search(query: String, page: Int = 1) {
        if (state.busy || !state.configured) return
        state = state.copy(query = query, page = page, busy = true)
        viewModelScope.launch {
            try {
                loadVariants()
            } catch (e: Exception) {
                connectionFailure(e)
            } finally {
                state = state.copy(busy = false)
            }
        }
    }

    fun refresh() {
        if (state.busy || !state.configured) return
        state = state.copy(busy = true)
        viewModelScope.launch {
            try {
                loadContext()
                loadVariants()
                state = state.copy(history = api().request("/history"))
            } catch (e: Exception) {
                connectionFailure(e)
            } finally {
                state = state.copy(busy = false)
            }
        }
    }

    private fun connectionFailure(e: Exception) {
        state =
            state.copy(
                online = false,
                message =
                    if (e is ApiFailure && e.status in listOf(401, 403))
                        "Accesso scaduto o revocato. Controlla la chiave nelle impostazioni."
                    else "Server non raggiungibile. Controlla la connessione.",
            )
    }

    fun add(variant: Variant, locationId: Int?) {
        if (!state.online || state.busy || state.pending) return
        val line = state.cart.find { it.variant.id == variant.id && it.locationId == locationId }
        val count = state.cart.filter { it.variant.id == variant.id }.sumOf { it.quantity }
        if (count >= variant.available) {
            state = state.copy(message = "Quantità disponibile esaurita.")
            return
        }
        state =
            state.copy(
                cart =
                    if (line == null) state.cart + CartLine(variant, 1, locationId)
                    else
                        state.cart.map {
                            if (it === line) it.copy(quantity = it.quantity + 1) else it
                        }
            )
    }

    fun remove(index: Int) {
        if (state.busy || state.pending) return
        state = state.copy(cart = state.cart.filterIndexed { i, _ -> i != index })
    }

    fun checkout(methodId: Int) {
        if (state.cart.isEmpty()) return
        val lines = JSONArray()
        state.cart.forEach {
            lines.put(
                JSONObject()
                    .put("variant_id", it.variant.id)
                    .put("quantity", it.quantity)
                    .put("location_id", it.locationId ?: JSONObject.NULL)
                    .put("expected_unit_price_cents", it.variant.price)
            )
        }
        operate("/sales", JSONObject().put("payment_method_id", methodId).put("lines", lines))
    }

    fun operate(path: String, payload: JSONObject) {
        if (!state.online || state.busy || state.pending) return
        payload.put("request_id", UUID.randomUUID().toString())
        val pending = JSONObject().put("path", path).put("payload", payload).put("url", state.url)
        try {
            val updated = JSONObject(saved.toString()).put("pending", pending)
            vault.write(updated)
            saved = updated
            state = state.copy(pending = true)
            retryPending()
        } catch (_: Exception) {
            state =
                state.copy(
                    message = "Impossibile salvare la richiesta. Nessuna operazione inviata."
                )
        }
    }

    private fun clearPending() {
        val updated = JSONObject(saved.toString()).apply { remove("pending") }
        vault.write(updated)
        saved = updated
    }

    fun retryPending() {
        if (!state.online || state.busy || !state.pending) return
        state = state.copy(busy = true)
        viewModelScope.launch {
            val pending = saved.getJSONObject("pending")
            try {
                check(pending.getString("url") == saved.getString("url")) {
                    "Negozio della richiesta non corrispondente."
                }
                val response =
                    try {
                        api()
                            .request(
                                "/operations/" +
                                    pending.getJSONObject("payload").getString("request_id")
                            )
                    } catch (lookup: ApiFailure) {
                        if (lookup.status != 404) throw lookup
                        api()
                            .request(
                                pending.getString("path"),
                                "POST",
                                pending.getJSONObject("payload"),
                            )
                    }
                clearPending()
                state =
                    state.copy(
                        pending = false,
                        cart =
                            if (pending.getString("path") == "/sales") emptyList() else state.cart,
                        message =
                            if (response.has("sale_id"))
                                "Operazione registrata · vendita #${response.getInt("sale_id")}"
                            else "Movimento registrato.",
                    )
            } catch (e: ApiFailure) {
                // Authentication/rate-limit failures say nothing about an
                // earlier request whose successful response may have been lost.
                if (e.status == 422) {
                    clearPending()
                    state =
                        state.copy(pending = false, message = e.message ?: "Operazione rifiutata.")
                } else
                    state =
                        state.copy(
                            message =
                                "Esito da verificare. Riprova la stessa richiesta: non verrà duplicata."
                        )
                if (e.status == 401 || e.status == 403) state = state.copy(online = false)
            } catch (_: Exception) {
                state =
                    state.copy(
                        online = false,
                        message =
                            "Esito non confermato. Quando torni online, riprova la stessa richiesta.",
                    )
            }
            try {
                if (state.online) {
                    loadContext()
                    loadVariants()
                }
            } catch (e: Exception) {
                connectionFailure(e)
            }
            state = state.copy(busy = false)
        }
    }

    fun saveResource(path: String, payload: JSONObject, update: Boolean = false) {
        if (!state.online || state.busy || state.pending) return
        state = state.copy(busy = true)
        viewModelScope.launch {
            try {
                api().request(path, if (update) "PATCH" else "POST", payload)
                state = state.copy(message = "Salvato.")
                loadContext()
                loadVariants()
            } catch (e: ApiFailure) {
                state = state.copy(message = e.message ?: "Salvataggio non riuscito.")
            } catch (e: Exception) {
                connectionFailure(e)
            } finally {
                state = state.copy(busy = false)
            }
        }
    }
}
