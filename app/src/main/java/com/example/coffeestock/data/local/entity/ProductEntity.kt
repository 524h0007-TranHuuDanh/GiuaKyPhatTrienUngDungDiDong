package com.example.coffeestock.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product")
data class ProductEntity(
    @PrimaryKey
    val id: String,

    val name: String,

    val unit: String = "kg",

    val minStock: Double = 0.0,

    val serverVersion: Long = 0,

    val updatedAt: String = ""
)