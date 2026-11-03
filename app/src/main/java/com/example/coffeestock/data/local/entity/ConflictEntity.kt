package com.example.coffeestock.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conflict_log")
data class ConflictEntity(
    @PrimaryKey(autoGenerate = true)
    val conflictId: Long = 0,

    val movementId: String,

    val localData: String,

    val serverData: String,

    val localRev: Long,

    val serverVersion: Long,

    val resolution: String,

    val createdAt: String
)