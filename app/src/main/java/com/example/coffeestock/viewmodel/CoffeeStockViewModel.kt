package com.example.coffeestock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.coffeestock.data.local.entity.MovementEntity
import com.example.coffeestock.data.repository.MovementRepository
import com.example.coffeestock.data.repository.ProductRepository
import com.example.coffeestock.data.repository.ProductStock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CoffeeStockUiState(
    val products: List<ProductStock> = emptyList(),
    val movements: List<MovementEntity> = emptyList(),
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val conflictCount: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class CoffeeStockViewModel(
    private val movementRepository: MovementRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    val uiState: StateFlow<CoffeeStockUiState> = combine(
        productRepository.getProductsWithStock(),
        movementRepository.getAllMovements(),
        movementRepository.observePendingCount(),
        movementRepository.observeFailedCount(),
        movementRepository.observeConflictCount()
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        CoffeeStockUiState(
            products = values[0] as List<ProductStock>,
            movements = values[1] as List<MovementEntity>,
            pendingCount = values[2] as Int,
            failedCount = values[3] as Int,
            conflictCount = values[4] as Int,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CoffeeStockUiState()
    )

    suspend fun createDraft(
        productId: String, type: String, quantity: Double,
        batchCode: String, partner: String, note: String
    ): String = movementRepository.createDraft(
        productId, type, quantity, batchCode, partner, note
    )

    suspend fun updateDraft(
        movementId: String, type: String, quantity: Double,
        batchCode: String, partner: String, note: String
    ) = movementRepository.updateDraft(
        movementId, type, quantity, batchCode, partner, note
    )

    suspend fun confirmMovement(movementId: String): Boolean =
        movementRepository.confirmMovement(movementId)

    suspend fun deleteDraft(movementId: String) =
        movementRepository.deleteDraft(movementId)

    suspend fun syncNow() = movementRepository.syncNow()

    class Factory(
        private val movementRepository: MovementRepository,
        private val productRepository: ProductRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CoffeeStockViewModel::class.java)) {
                return CoffeeStockViewModel(
                    movementRepository,
                    productRepository
                ) as T
            }
            throw IllegalArgumentException(
                "Unknown ViewModel class: ${modelClass.name}"
            )
        }
    }
}