package com.example.coffeestock.sync

import android.content.Context
import com.example.coffeestock.BuildConfig
import com.example.coffeestock.data.local.AppDatabase
import com.example.coffeestock.data.remote.RetrofitClient

object SyncEngineFactory {

    fun create(context: Context): SyncEngine {

        val database = AppDatabase.getDatabase(
            context.applicationContext
        )

        return SyncEngine(
            database = database,
            movementDao = database.movementDao(),
            syncDao = database.syncDao(),
            apiService = RetrofitClient.apiService,
            apiKey = BuildConfig.API_KEY,
            pullDataApplier = PullDataApplier(database)
        )
    }
}