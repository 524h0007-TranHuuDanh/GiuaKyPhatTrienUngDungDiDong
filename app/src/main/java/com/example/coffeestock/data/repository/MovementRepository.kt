package com.example.coffeestock.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.coffeestock.data.local.AppDatabase
import com.example.coffeestock.data.local.dao.MovementDao
import com.example.coffeestock.data.local.dao.ProductDao
import com.example.coffeestock.data.local.entity.MovementEntity
import com.example.coffeestock.sync.SyncScheduler
import com.example.coffeestock.util.Clock
import java.util.UUID

class MovementRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val movementDao: MovementDao,
    private val productDao: ProductDao
) {

    suspend fun createDraft(
        productId: String,
        type: String,
        quantity: Double,
        batchCode: String = "",
        partner: String = "",
        note: String = ""
    ): String {

        validateMovement(type, quantity, batchCode)

        val movementId = UUID.randomUUID().toString()
        val currentTime = Clock.nowUtc()

        database.withTransaction {

            val product = productDao.getProductById(productId)
                ?: throw IllegalArgumentException(
                    "Không tìm thấy mặt hàng"
                )

            movementDao.insertMovement(
                MovementEntity(
                    id = movementId,
                    productId = productId,
                    productName = product.name,
                    type = type,
                    quantity = quantity,
                    batchCode = batchCode,
                    partner = partner,
                    note = note,
                    status = "DRAFT",
                    serverVersion = 0,
                    syncState = "PENDING",
                    pendingOp = "CREATE",
                    localRev = 1,
                    mutationId = UUID.randomUUID().toString(),
                    isDeleted = false,
                    createdAt = currentTime,
                    updatedAt = currentTime
                )
            )
        }

        SyncScheduler.enqueueSync(context)

        return movementId
    }

    suspend fun updateDraft(
        movementId: String,
        type: String,
        quantity: Double,
        batchCode: String,
        partner: String,
        note: String
    ) {

        validateMovement(type, quantity, batchCode)

        database.withTransaction {

            val movement = movementDao.getMovementById(movementId)
                ?: throw IllegalArgumentException(
                    "Không tìm thấy phiếu"
                )

            require(
                movement.status == "DRAFT" &&
                        !movement.isDeleted
            ) {
                "Chỉ được sửa phiếu nháp chưa bị xóa"
            }

            movementDao.insertMovement(
                movement.copy(
                    type = type,
                    quantity = quantity,
                    batchCode = batchCode,
                    partner = partner,
                    note = note,
                    syncState = "PENDING",
                    pendingOp = if (movement.serverVersion == 0L) {
                        "CREATE"
                    } else {
                        "UPDATE"
                    },
                    localRev = movement.localRev + 1,
                    mutationId = UUID.randomUUID().toString(),
                    updatedAt = Clock.nowUtc()
                )
            )
        }

        SyncScheduler.enqueueSync(context)
    }

    suspend fun confirmMovement(
        movementId: String
    ): Boolean {

        val insufficientStock = database.withTransaction {

            val movement = movementDao.getMovementById(movementId)
                ?: throw IllegalArgumentException(
                    "Không tìm thấy phiếu kho."
                )

            if (movement.isDeleted) {
                throw IllegalStateException(
                    "Phiếu đã bị xóa."
                )
            }

            if (movement.status != "DRAFT") {
                throw IllegalStateException(
                    "Chỉ được xác nhận phiếu nháp."
                )
            }

            require(movement.quantity > 0) {
                "Số lượng phải lớn hơn 0."
            }

            var stockIsInsufficient = false

            if (movement.type == "OUT") {

                val confirmedMovements =
                    movementDao.getConfirmedMovementsByProductOnce(
                        movement.productId
                    )

                val totalIn = confirmedMovements
                    .filter { it.type == "IN" }
                    .sumOf { it.quantity }

                val totalOut = confirmedMovements
                    .filter { it.type == "OUT" }
                    .sumOf { it.quantity }

                val currentStock = totalIn - totalOut

                stockIsInsufficient =
                    movement.quantity > currentStock
            }

            movementDao.insertMovement(
                movement.copy(
                    status = "CONFIRMED",
                    syncState = "PENDING",
                    pendingOp = if (movement.serverVersion == 0L) {
                        "CREATE"
                    } else {
                        "UPDATE"
                    },
                    localRev = movement.localRev + 1,
                    mutationId = UUID.randomUUID().toString(),
                    updatedAt = Clock.nowUtc()
                )
            )

            stockIsInsufficient
        }

        SyncScheduler.enqueueSync(context)

        return insufficientStock
    }

    suspend fun deleteDraft(
        movementId: String
    ) {

        val needsSync = database.withTransaction {

            val movement = movementDao.getMovementById(movementId)
                ?: throw IllegalArgumentException(
                    "Không tìm thấy phiếu"
                )

            require(
                movement.status == "DRAFT" &&
                        !movement.isDeleted
            ) {
                "Không thể xóa phiếu này"
            }

            check(movement.syncState != "SYNCING") {
                "Phiếu đang đồng bộ, vui lòng thử lại sau"
            }

            if (movement.serverVersion == 0L) {

                movementDao.deleteMovementPermanently(
                    movementId
                )

                false

            } else {

                movementDao.insertMovement(
                    movement.copy(
                        isDeleted = true,
                        syncState = "PENDING",
                        pendingOp = "DELETE",
                        localRev = movement.localRev + 1,
                        mutationId = UUID.randomUUID().toString(),
                        updatedAt = Clock.nowUtc()
                    )
                )

                true
            }
        }

        if (needsSync) {
            SyncScheduler.enqueueSync(context)
        }
    }

    private fun validateMovement(
        type: String,
        quantity: Double,
        batchCode: String
    ) {

        require(type == "IN" || type == "OUT") {
            "Loại phiếu không hợp lệ"
        }

        require(quantity.isFinite() && quantity > 0) {
            "Số lượng phải lớn hơn 0"
        }

        require(type != "IN" || batchCode.isNotBlank()) {
            "Phiếu nhập bắt buộc có mã lô"
        }
    }

    fun getAllMovements() =
        movementDao.getAllMovements()

    suspend fun getMovementById(
        movementId: String
    ) = movementDao.getMovementById(movementId)

    fun observePendingCount() =
        movementDao.observePendingCount()

    fun observeFailedCount() =
        movementDao.observeFailedCount()

    fun observeConflictCount() =
        movementDao.observeConflictCount()

    suspend fun syncNow() {

        database.withTransaction {
            movementDao.requeueFailed()
            movementDao.resetStuckSyncing()
        }

        SyncScheduler.enqueueSync(context)
    }
}