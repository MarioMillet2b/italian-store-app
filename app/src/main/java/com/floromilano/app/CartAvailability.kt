package com.floromilano.app

object CartAvailability {
    fun remaining(variant: Variant, cart: List<CartLine>, locationId: Int?): Int {
        val total =
            variant.available - cart.filter { it.variant.id == variant.id }.sumOf { it.quantity }
        val location =
            locationId?.let { id ->
                val units = variant.locations.firstOrNull { it.first == id }?.second ?: 0
                units -
                    cart
                        .filter { it.variant.id == variant.id && it.locationId == id }
                        .sumOf { it.quantity }
            }
        return maxOf(0, if (location == null) total else minOf(total, location))
    }
}
