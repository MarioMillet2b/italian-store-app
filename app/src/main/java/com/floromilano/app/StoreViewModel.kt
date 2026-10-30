package com.floromilano.app

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
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
    val imageUrl: String? = null,
    val categoryId: Int? = null,
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
    val products: List<CatalogProduct> = emptyList(),
    val categories: List<CatalogCategory> = emptyList(),
    val categoryId: Int? = null,
    val inStockOnly: Boolean = false,
    val lowStockOnly: Boolean = false,
    val catalogMode: String = "sale",
    val sort: String = "name",
    val catalogLoading: Boolean = false,
    val totalProducts: Int = 0,
)

class StoreViewModel(application: Application) : AndroidViewModel(application) {
    private val vault = (application as StoreApplication).vault
    private var vaultFailed = false
    private var sessionVersion = 0
    private var catalogVersion = 0

    private fun tr(id: Int, vararg args: Any) = AppLanguage.text(getApplication(), id, *args)

    private var foreground = false
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
                message = if (vaultFailed) tr(R.string.vault_unreadable) else "",
            )
        )
        private set

    private fun api(): StoreGateway =
        getApplication<StoreApplication>().gatewayOverride
            ?: StoreApi(
                saved.getString("url"),
                saved.getString("token"),
                AppLanguage.locale(getApplication()).language,
            ) {
                tr(R.string.api_error, it)
            }

    private val connectivity = application.getSystemService(ConnectivityManager::class.java)
    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                viewModelScope.launch { state = state.copy(online = false) }
            }
        }

    fun setForeground(visible: Boolean) {
        foreground = visible
        if (visible) refresh()
    }

    override fun onCleared() {
        connectivity.unregisterNetworkCallback(networkCallback)
        super.onCleared()
    }

    init {
        connectivity.registerDefaultNetworkCallback(networkCallback)
        viewModelScope.launch {
            while (true) {
                if (foreground && state.configured && !state.busy) {
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
                    tr(R.string.pending_store_change)
                }
                val credential =
                    token.trim().ifBlank {
                        if (normalized == saved.optString("url")) saved.optString("token") else ""
                    }
                require(credential.isNotBlank()) { tr(R.string.key_required) }
                val candidate =
                    getApplication<StoreApplication>().gatewayOverride
                        ?: StoreApi(
                            normalized,
                            credential,
                            AppLanguage.locale(getApplication()).language,
                        ) {
                            tr(R.string.api_error, it)
                        }
                val context = candidate.request("/context")
                require(context.optInt("api_version") == 1) { tr(R.string.incompatible_api) }
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
                        message = tr(R.string.store_connected),
                    )
                applyContext(context)
                loadVariants()
            } catch (e: Exception) {
                state =
                    state.copy(
                        message =
                            if (e is ApiFailure) e.message ?: tr(R.string.connection_failed)
                            else tr(R.string.invalid_connection)
                    )
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
                categories =
                    json.optJSONArray("categories")?.let { values ->
                        (0 until values.length()).map { index ->
                            values.getJSONObject(index).let { item ->
                                CatalogCategory(
                                    item.getInt("id"),
                                    item.getString("name"),
                                    if (item.isNull("parent_id")) null
                                    else item.getInt("parent_id"),
                                    item.optInt("product_count", 1),
                                    item.optInt("sale_product_count", 1),
                                )
                            }
                        }
                    } ?: emptyList(),
            )
    }

    private suspend fun loadContext() {
        val version = sessionVersion
        val response = api().request("/context")
        if (version == sessionVersion) applyContext(response)
    }

    private suspend fun loadVariants(requestVersion: Int = ++catalogVersion) {
        val version = sessionVersion
        val query = URLEncoder.encode(state.query, "UTF-8")
        val category =
            state.categoryId?.let { if (it == -1) "&uncategorized=1" else "&category_id=$it" } ?: ""
        val page = state.page
        val response =
            api()
                .request(
                    "/catalog?q=$query&page=$page$category&mode=${state.catalogMode}&sort=${state.sort}&in_stock=${if(state.inStockOnly) 1 else 0}&low_stock=${if(state.lowStockOnly) 1 else 0}"
                )
        if (version != sessionVersion || requestVersion != catalogVersion) return
        val incoming = CatalogParser.products(response)
        val products = if (page == 1) incoming else (state.products + incoming).distinctBy { it.id }
        state =
            state.copy(
                products = products,
                variants = products.flatMap { it.variants },
                lastPage = response.getInt("last_page"),
                totalProducts = response.getInt("total"),
                online = true,
            )
    }

    fun search(query: String, page: Int = 1) {
        browse(query = query, page = page)
    }

    fun browse(
        query: String = state.query,
        categoryId: Int? = state.categoryId,
        inStock: Boolean = state.inStockOnly,
        lowStock: Boolean = state.lowStockOnly,
        sort: String = state.sort,
        mode: String = state.catalogMode,
        page: Int = 1,
    ) {
        if (state.busy || !state.configured) return
        val version = sessionVersion
        val requestVersion = ++catalogVersion
        state =
            state.copy(
                query = query.take(100),
                categoryId = categoryId,
                inStockOnly = inStock,
                lowStockOnly = lowStock,
                sort = sort,
                catalogMode = mode,
                page = page,
                catalogLoading = true,
            )
        viewModelScope.launch {
            try {
                loadVariants(requestVersion)
            } catch (e: Exception) {
                if (version == sessionVersion && requestVersion == catalogVersion)
                    if (e is ApiFailure && e.status == 422)
                        state = state.copy(message = e.message.orEmpty())
                    else connectionFailure(e)
            } finally {
                if (version == sessionVersion && requestVersion == catalogVersion)
                    state = state.copy(catalogLoading = false)
            }
        }
    }

    fun languageChanged() {
        sessionVersion++
        catalogVersion++
        state = state.copy(message = "", page = 1, catalogLoading = false)
        refresh()
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
                state = state.copy(busy = false, catalogLoading = false)
            }
        }
    }

    private fun connectionFailure(e: Exception) {
        state =
            state.copy(
                online = false,
                message =
                    if (e is ApiFailure && e.status in listOf(401, 403)) tr(R.string.access_expired)
                    else tr(R.string.no_connection),
            )
    }

    fun add(variant: Variant, locationId: Int?, quantity: Int = 1) {
        if (!state.online || state.busy || state.pending) return
        val line = state.cart.find { it.variant.id == variant.id && it.locationId == locationId }
        if (
            quantity < 1 || quantity > CartAvailability.remaining(variant, state.cart, locationId)
        ) {
            state = state.copy(message = tr(R.string.stock_exhausted))
            return
        }
        state =
            state.copy(
                cart =
                    if (line == null) state.cart + CartLine(variant, quantity, locationId)
                    else
                        state.cart.map {
                            if (it === line) it.copy(quantity = it.quantity + quantity) else it
                        }
            )
    }

    fun reduce(index: Int) {
        if (state.busy || state.pending) return
        val line = state.cart.getOrNull(index) ?: return
        if (line.quantity == 1) remove(index)
        else
            state =
                state.copy(
                    cart =
                        state.cart.mapIndexed { i, item ->
                            if (i == index) item.copy(quantity = item.quantity - 1) else item
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
            state = state.copy(message = tr(R.string.save_request_failed))
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
                                tr(R.string.sale_saved, response.getInt("sale_id"))
                            else tr(R.string.operation_saved),
                    )
            } catch (e: ApiFailure) {
                // Authentication/rate-limit failures say nothing about an
                // earlier request whose successful response may have been lost.
                if (e.status == 422) {
                    clearPending()
                    state =
                        state.copy(
                            pending = false,
                            message = e.message ?: tr(R.string.operation_rejected),
                        )
                } else state = state.copy(message = tr(R.string.pending_retry))
                if (e.status == 401 || e.status == 403) state = state.copy(online = false)
            } catch (_: Exception) {
                state = state.copy(online = false, message = tr(R.string.pending_offline))
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
                state = state.copy(message = tr(R.string.saved))
                loadContext()
                loadVariants()
            } catch (e: ApiFailure) {
                if (e.status == 401 || e.status == 403) connectionFailure(e)
                else state = state.copy(message = e.message ?: tr(R.string.save_failed))
            } catch (e: Exception) {
                connectionFailure(e)
            } finally {
                state = state.copy(busy = false)
            }
        }
    }
}
