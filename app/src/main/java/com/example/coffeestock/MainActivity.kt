package com.example.coffeestock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.coffeestock.data.local.AppDatabase
import com.example.coffeestock.data.local.seed.ProductSeeder
import com.example.coffeestock.data.repository.MovementRepository
import com.example.coffeestock.data.repository.ProductRepository
import com.example.coffeestock.sync.SyncScheduler
import com.example.coffeestock.ui.MainScreen
import com.example.coffeestock.viewmodel.CoffeeStockViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(
            applicationContext
        )

        val productRepository = ProductRepository(
            database.productDao(),
            database.movementDao()
        )

        val movementRepository = MovementRepository(
            context = applicationContext,
            database = database,
            movementDao = database.movementDao(),
            productDao = database.productDao()
        )

        val viewModel = ViewModelProvider(
            this,
            CoffeeStockViewModel.Factory(
                movementRepository = movementRepository,
                productRepository = productRepository
            )
        )[CoffeeStockViewModel::class.java]

        lifecycleScope.launch {

            ProductSeeder(
                database.productDao()
            ).seedIfEmpty()

            SyncScheduler.enqueueSync(
                applicationContext
            )
        }

        setContent {
            MaterialTheme {
                Surface {
                    MainScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}