package com.floromilano.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.net.URI
import java.net.URLDecoder
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Emulator-only, synthetic gateway. Never reads or writes a real shop. */
class UiExperienceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private var initialized = false
    private lateinit var vm: StoreViewModel

    @After
    fun cleanup() {
        if (!initialized) return
        compose.runOnUiThread {
            vm.setForeground(false)
            (compose.activity.application as StoreApplication).apply {
                vault.write(JSONObject())
                gatewayOverride = null
            }
        }
    }

    @Test
    fun browseChooseVariantsKeepCartAndSwitchLanguages() {
        assumeTrue("UI fixtures are restricted to Android emulators", Build.HARDWARE == "ranchu")
        val app = compose.activity.application as StoreApplication
        val gateway = CatalogueFixture(app)
        compose.runOnUiThread {
            initialized = true
            app.gatewayOverride = gateway
            vm = ViewModelProvider(compose.activity)[StoreViewModel::class.java]
            val assets = InstrumentationRegistry.getInstrumentation().context.assets
            for (name in listOf("black", "blue")) {
                ProductImages.cache.put(
                    "https://fixture.invalid/$name.jpg",
                    assets.open("floro-$name.jpg").use { BitmapFactory.decodeStream(it) },
                )
            }
            vm.configure("https://fixture.invalid", "instrumentation-only-key")
        }
        waitCatalog(3)
        capture("01-it-catalogue")
        compose.onNodeWithTag("category-1").performClick()
        waitCatalog(2)
        compose.onNodeWithTag("product-card-2").assertDoesNotExist()
        capture("02-it-category")
        compose.onNodeWithTag("category-all").performClick()
        waitCatalog(3)
        compose.onNodeWithTag("product-card-1").performClick()
        compose.onNodeWithTag("variant-2", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Aumenta quantità").performClick()
        capture("03-it-variant")
        compose.onNodeWithTag("add-selected-variant").performClick()
        compose.waitUntil(30000) { vm.state.cart.sumOf { it.quantity } == 2 }
        compose.onNodeWithTag("product-card-2").performClick()
        compose.onNodeWithTag("add-selected-variant").performClick()
        compose.waitUntil(30000) { vm.state.cart.size == 2 }
        assertEquals(29500L, vm.state.cart.sumOf { it.variant.price * it.quantity })
        compose.onNodeWithTag("view-cart").performClick()
        capture("04-it-cart")
        compose.onNodeWithContentDescription("Chiudi").performClick()
        compose.onNodeWithTag("nav-3").performClick()
        compose.onNodeWithTag("language-es", useUnmergedTree = true).performClick()
        compose.waitUntil(30000) {
            compose.onAllNodesWithText("Ajustes").fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitUntil(30000) { !vm.state.busy }
        assertEquals(3, vm.state.cart.sumOf { it.quantity })
        capture("05-es-settings")
        compose.onNodeWithTag("nav-0").performClick()
        waitCatalog(3)
        capture("06-es-catalogue")
        compose.onNodeWithTag("view-cart").performClick()
        compose.onNodeWithTag("register-sale").assertIsNotEnabled() // No payment selected.
        capture("07-es-cart")
        compose.onNodeWithContentDescription("Cerrar").performClick()
        compose.onNodeWithTag("catalog-search").performTextInput("Camisa")
        waitCatalog(1)
        compose.onNodeWithTag("product-card-2").assertExists()
        compose.onNodeWithTag("product-card-1").assertDoesNotExist()
        capture("08-es-search")
        compose.onNodeWithContentDescription("Limpiar búsqueda").performClick()
        waitCatalog(3)
        assertEquals(0, gateway.writes)
    }

    private fun waitCatalog(size: Int) {
        compose.waitUntil(60000) {
            vm.state.products.size == size && !vm.state.catalogLoading && !vm.state.busy
        }
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val directory =
            File(compose.activity.getExternalFilesDir(null), "ux-audit").apply { mkdirs() }
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(directory, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}

private class CatalogueFixture(private val app: StoreApplication) : StoreGateway {
    var writes = 0

    override suspend fun request(path: String, method: String, payload: JSONObject?): JSONObject {
        if (method != "GET") {
            writes++
            error("UI tests cannot submit sales or stock operations")
        }
        val es = AppLanguage.locale(app).language == "es"
        val uri = URI("https://fixture.invalid$path")
        if (uri.path == "/context")
            return JSONObject()
                .put("api_version", 1)
                .put("store_name", "Floro Milano")
                .put("currency", "EUR")
                .put("locations", JSONArray())
                .put("metrics", JSONObject().put("units", 25))
                .put(
                    "categories",
                    JSONArray()
                        .put(
                            JSONObject()
                                .put("id", 1)
                                .put("name", if (es) "Hombre" else "Uomo")
                                .put("parent_id", JSONObject.NULL)
                        )
                        .put(
                            JSONObject()
                                .put("id", 2)
                                .put("name", if (es) "Mujer" else "Donna")
                                .put("parent_id", JSONObject.NULL)
                        ),
                )
                .put(
                    "payment_methods",
                    JSONArray()
                        .put(
                            JSONObject()
                                .put("id", 1)
                                .put("name", if (es) "Efectivo" else "Contanti")
                                .put("active", true)
                        ),
                )
        if (uri.path == "/history")
            return JSONObject().put("sales", JSONArray()).put("movements", JSONArray())
        check(uri.path == "/catalog")
        val query =
            uri.rawQuery
                .orEmpty()
                .split('&')
                .mapNotNull { piece ->
                    piece
                        .split('=', limit = 2)
                        .takeIf { it.size == 2 }
                        ?.let { it[0] to URLDecoder.decode(it[1], "UTF-8") }
                }
                .toMap()
        val products =
            listOf(
                    product(
                        1,
                        "Polo",
                        1,
                        es,
                        listOf(
                            variant(1, 1, "Polo", 9500, "M", "black", es),
                            variant(2, 1, "Polo", 11000, "L", "blue", es),
                        ),
                    ),
                    product(
                        2,
                        if (es) "Camisa" else "Camicia",
                        2,
                        es,
                        listOf(
                            variant(3, 2, if (es) "Camisa" else "Camicia", 7500, "M", "blue", es)
                        ),
                    ),
                    product(
                        3,
                        if (es) "Jersey" else "Maglia",
                        1,
                        es,
                        listOf(
                            variant(4, 3, if (es) "Jersey" else "Maglia", 9500, "M", "black", es)
                        ),
                    ),
                )
                .filter { p ->
                    (query["category_id"] == null ||
                        query["category_id"] == p.getInt("category_id").toString()) &&
                        (query["q"].isNullOrBlank() ||
                            p.getString("name").contains(query["q"].orEmpty(), ignoreCase = true))
                }
        return JSONObject()
            .put("data", JSONArray(products))
            .put("current_page", 1)
            .put("last_page", 1)
            .put("total", products.size)
    }

    private fun product(
        id: Int,
        name: String,
        category: Int,
        es: Boolean,
        variants: List<JSONObject>,
    ) =
        JSONObject()
            .put("id", id)
            .put("name", name)
            .put("category_id", category)
            .put(
                "category_name",
                if (category == 1) if (es) "Hombre" else "Uomo" else if (es) "Mujer" else "Donna",
            )
            .put("variants", JSONArray(variants))

    private fun variant(
        id: Int,
        product: Int,
        name: String,
        price: Int,
        size: String,
        image: String,
        es: Boolean,
    ) =
        JSONObject()
            .put("id", id)
            .put("product_id", product)
            .put("name", name)
            .put("sku", "FL-$id")
            .put("barcode", "FMV00000000$id")
            .put("stock", 5)
            .put("available", 5)
            .put("unassigned", 5)
            .put("low_stock_threshold", 2)
            .put("status", "published")
            .put("locations", JSONArray())
            .put("unit_price_cents", price)
            .put("attributes", JSONObject().put("Taglia", size))
            .put("display_attributes", JSONObject().put(if (es) "Talla" else "Taglia", size))
            .put("image", "https://fixture.invalid/$image.jpg")
}
