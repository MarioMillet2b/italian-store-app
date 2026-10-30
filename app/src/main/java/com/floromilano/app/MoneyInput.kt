package com.floromilano.app

import java.math.BigDecimal
import java.math.RoundingMode

internal fun priceCents(value: String): Long? =
    try {
        BigDecimal(value.trim().replace(',', '.'))
            .setScale(2, RoundingMode.UNNECESSARY)
            .movePointRight(2)
            .longValueExact()
            .takeIf { it in 0..100000000 }
    } catch (_: Exception) {
        null
    }
