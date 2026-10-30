package com.floromilano.app

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CatalogExperienceTest {
    @Test
    fun groupedCardsKeepVariantIdentityPricesAndLocalizedAttributes() {
        val first = variantJson(1, 9500, 2)
        val second = variantJson(2, 11000, 3)
        val json =
            JSONObject()
                .put(
                    "data",
                    JSONArray()
                        .put(
                            JSONObject()
                                .put("id", 1)
                                .put("name", "Polo")
                                .put("category_id", 4)
                                .put("category_name", "Hombre")
                                .put("variants", JSONArray().put(first).put(second))
                        ),
                )
        val product = CatalogParser.products(json).single()
        assertEquals(2, product.variants.size)
        assertEquals(9500L, product.minimumPrice)
        assertEquals(11000L, product.maximumPrice)
        assertEquals(5, product.available)
        assertEquals("Talla: M", product.variants.first().attributes)
        assertEquals("https://example.invalid/photo.jpg", product.imageUrl)
    }

    @Test
    fun quantitiesRespectBothGlobalReservationsAndChosenLocation() {
        val v =
            CatalogParser.variant(variantJson(1, 9500, 5)).copy(locations = listOf(1 to 2, 2 to 3))
        val cart = listOf(CartLine(v, 1, 1), CartLine(v, 2, null))
        assertEquals(2, CartAvailability.remaining(v, cart, null))
        assertEquals(1, CartAvailability.remaining(v, cart, 1))
        assertEquals(2, CartAvailability.remaining(v, cart, 2))
        assertEquals(0, CartAvailability.remaining(v, cart, 99))
        val otherSize = v.copy(id = 2)
        assertEquals(5, CartAvailability.remaining(otherSize, cart, null))
    }

    @Test
    fun spanishAndItalianHaveTheSameStringsAndFormatArguments() {
        fun strings(folder: String): Map<String, String> {
            val file = File("src/main/res/$folder/strings.xml")
            val nodes =
                DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(file)
                    .getElementsByTagName("string")
            val messages =
                (0 until nodes.length)
                    .associate { i ->
                        nodes.item(i).attributes.getNamedItem("name").nodeValue to
                            nodes.item(i).textContent
                    }
                    .toMutableMap()
            val plurals =
                DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(file)
                    .getElementsByTagName("plurals")
            for (i in 0 until plurals.length) {
                val plural = plurals.item(i)
                val name = plural.attributes.getNamedItem("name").nodeValue
                for (j in 0 until plural.childNodes.length) {
                    val item = plural.childNodes.item(j)
                    if (item.nodeName == "item")
                        messages["$name.${item.attributes.getNamedItem("quantity").nodeValue}"] =
                            item.textContent
                }
            }
            return messages
        }
        val it = strings("values")
        val es = strings("values-es")
        assertEquals(it.keys, es.keys)
        val argument = Regex("%[0-9]+\\$[ds]")
        for (key in it.keys) {
            assertTrue(key, es.getValue(key).isNotBlank())
            assertEquals(
                key,
                argument.findAll(it.getValue(key)).map { it.value }.toList().sorted(),
                argument.findAll(es.getValue(key)).map { it.value }.toList().sorted(),
            )
        }
    }

    private fun variantJson(id: Int, price: Long, available: Int) =
        JSONObject()
            .put("id", id)
            .put("product_id", 1)
            .put("name", "Polo")
            .put("sku", "POLO-$id")
            .put("barcode", "FMV$id")
            .put("attributes", JSONObject().put("Taglia", "M"))
            .put("display_attributes", JSONObject().put("Talla", "M"))
            .put("unit_price_cents", price)
            .put("stock", 5)
            .put("available", available)
            .put("unassigned", 5)
            .put("low_stock_threshold", 2)
            .put("locations", JSONArray())
            .put("status", "published")
            .put("image", "https://example.invalid/photo.jpg")
}
