package com.example.coffeestock.data.local.seed

import com.example.coffeestock.data.local.dao.ProductDao
import com.example.coffeestock.data.local.entity.ProductEntity

class ProductSeeder(
    private val productDao: ProductDao
) {

    suspend fun seedIfEmpty() {
        if (productDao.getAllProductsOnce().isEmpty()) {
            val products = listOf(
                ProductEntity(
                    id = "product-001",
                    name = "Cà phê Arabica",
                    unit = "kg",
                    minStock = 10.0
                ),
                ProductEntity(
                    id = "product-002",
                    name = "Cà phê Robusta",
                    unit = "kg",
                    minStock = 10.0
                ),
                ProductEntity(
                    id = "product-003",
                    name = "Cà phê Culi",
                    unit = "kg",
                    minStock = 5.0
                )
            )

            productDao.insertProducts(products)
        }
    }
}