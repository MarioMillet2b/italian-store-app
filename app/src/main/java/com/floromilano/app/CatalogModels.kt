package com.floromilano.app

data class CatalogCategory(
    val id: Int,
    val name: String,
    val parentId: Int? = null,
    val productCount: Int = 1,
    val saleProductCount: Int = 1,
)

data class CatalogProduct(
    val id: Int,
    val name: String,
    val categoryId: Int?,
    val categoryName: String,
    val variants: List<Variant>,
) {
    val imageUrl
        get() = variants.firstNotNullOfOrNull { it.imageUrl }

    val minimumPrice
        get() = variants.minOfOrNull { it.price } ?: 0L

    val maximumPrice
        get() = variants.maxOfOrNull { it.price } ?: 0L

    val available
        get() = variants.sumOf { it.available }

    val stock
        get() = variants.sumOf { it.stock }

    val lowStock
        get() = variants.any { it.stock <= it.lowStock }
}
