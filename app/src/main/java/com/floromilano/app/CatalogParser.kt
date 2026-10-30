package com.floromilano.app

import org.json.JSONObject

object CatalogParser {
    fun products(response: JSONObject): List<CatalogProduct> {
        val rows = response.getJSONArray("data")
        return (0 until rows.length()).map { index ->
            val p = rows.getJSONObject(index)
            val values = p.getJSONArray("variants")
            CatalogProduct(
                p.getInt("id"),
                p.getString("name"),
                if (p.isNull("category_id")) null else p.getInt("category_id"),
                p.optString("category_name", ""),
                (0 until values.length()).map { variant(values.getJSONObject(it)) },
            )
        }
    }

    fun variant(v: JSONObject): Variant {
        val attrs =
            v.optJSONObject("display_attributes") ?: v.optJSONObject("attributes") ?: JSONObject()
        val balances = v.getJSONArray("locations")
        return Variant(
            v.getInt("id"),
            v.getInt("product_id"),
            v.getString("name"),
            v.getString("sku"),
            v.getString("barcode"),
            attrs.keys().asSequence().joinToString(" · ") { key ->
                "$key: ${attrs.optString(key)}"
            },
            v.getLong("unit_price_cents"),
            v.getInt("stock"),
            v.getInt("available"),
            v.getInt("unassigned"),
            v.getInt("low_stock_threshold"),
            (0 until balances.length()).map { index ->
                balances.getJSONObject(index).let { b ->
                    b.getInt("inventory_location_id") to b.getInt("quantity")
                }
            },
            v.getString("status"),
            v.optString("image", "").takeIf { it.isNotBlank() },
            if (v.isNull("category_id")) null else v.getInt("category_id"),
        )
    }
}
