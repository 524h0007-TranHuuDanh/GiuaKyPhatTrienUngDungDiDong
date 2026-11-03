package com.example.coffeestock.sync

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.coffeestock.data.local.AppDatabase
import com.example.coffeestock.data.local.entity.MovementEntity
import com.example.coffeestock.data.remote.dto.PullResponseDto
import com.example.coffeestock.data.remote.dto.RemoteMovementDto
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PullDataApplierTest {

    private lateinit var database: AppDatabase
    private lateinit var applier: PullDataApplier

    @Before
    fun setUp() {
        val context =
            InstrumentationRegistry.getInstrumentation().targetContext

        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        applier = PullDataApplier(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createRemoteMovement(
        id: String = "movement-001",
        quantity: Double = 15.0,
        serverVersion: Long = 1L,
        serverSeq: Long = 1L,
        isDeleted: Boolean = false
    ): RemoteMovementDto {
        return RemoteMovementDto(
            id = id,
            productId = "product-001",
            productName = "Ca phe Arabica",
            type = "IN",
            quantity = quantity,
            batchCode = "BATCH-001",
            partner = "Nha cung cap A",
            note = "Server movement",
            status = "DRAFT",
            serverVersion = serverVersion,
            serverSeq = serverSeq,
            isDeleted = isDeleted,
            createdAt = "2026-11-03T01:00:00.000Z",
            updatedAt = "2026-11-03T02:00:00.000Z"
        )
    }

    private fun createLocalMovement(
        id: String = "movement-001",
        quantity: Double = 10.0,
        serverVersion: Long = 0L,
        pendingOp: String = "NONE",
        syncState: String = "SYNCED"
    ): MovementEntity {
        return MovementEntity(
            id = id,
            productId = "product-001",
            productName = "Ca phe Arabica",
            type = "IN",
            quantity = quantity,
            batchCode = "BATCH-001",
            partner = "Nha cung cap A",
            note = "Local movement",
            status = "DRAFT",
            serverVersion = serverVersion,
            syncState = syncState,
            pendingOp = pendingOp,
            localRev = if (pendingOp == "NONE") 0L else 2L,
            mutationId = "mutation-local-001",
            isDeleted = false,
            createdAt = "2026-11-03T01:00:00.000Z",
            updatedAt = "2026-11-03T01:00:00.000Z"
        )
    }

    @Test
    fun applyPage_shouldInsertNewServerMovementAndSaveCursor() =
        runBlocking {

            val response = PullResponseDto(
                items = listOf(
                    createRemoteMovement(
                        id = "movement-001",
                        quantity = 15.0,
                        serverVersion = 1L,
                        serverSeq = 1L
                    )
                ),
                nextCursor = 1L,
                hasMore = false
            )

            applier.applyPage(response)

            val saved = database.movementDao()
                .getMovementById("movement-001")

            assertNotNull(saved)

            assertEquals(
                15.0,
                saved!!.quantity,
                0.0
            )

            assertEquals(
                1L,
                saved.serverVersion
            )

            assertEquals(
                "SYNCED",
                saved.syncState
            )

            assertEquals(
                "NONE",
                saved.pendingOp
            )

            assertEquals(
                1L,
                applier.getCurrentCursor()
            )
        }

    @Test
    fun applyPage_shouldUpdateExistingSyncedMovement() =
        runBlocking {

            database.movementDao().insertMovement(
                createLocalMovement(
                    quantity = 10.0,
                    serverVersion = 1L
                )
            )

            val response = PullResponseDto(
                items = listOf(
                    createRemoteMovement(
                        quantity = 20.0,
                        serverVersion = 2L,
                        serverSeq = 2L
                    )
                ),
                nextCursor = 2L,
                hasMore = false
            )

            applier.applyPage(response)

            val saved = database.movementDao()
                .getMovementById("movement-001")

            assertNotNull(saved)

            assertEquals(
                20.0,
                saved!!.quantity,
                0.0
            )

            assertEquals(
                2L,
                saved.serverVersion
            )

            assertEquals(
                2L,
                applier.getCurrentCursor()
            )
        }

    @Test
    fun applyPage_shouldNotOverwritePendingLocalMovement() =
        runBlocking {

            database.movementDao().insertMovement(
                createLocalMovement(
                    quantity = 99.0,
                    serverVersion = 1L,
                    pendingOp = "UPDATE",
                    syncState = "PENDING"
                )
            )

            val response = PullResponseDto(
                items = listOf(
                    createRemoteMovement(
                        quantity = 20.0,
                        serverVersion = 2L,
                        serverSeq = 2L
                    )
                ),
                nextCursor = 2L,
                hasMore = false
            )

            applier.applyPage(response)

            val saved = database.movementDao()
                .getMovementById("movement-001")

            assertNotNull(saved)

            assertEquals(
                99.0,
                saved!!.quantity,
                0.0
            )

            assertEquals(
                "UPDATE",
                saved.pendingOp
            )

            assertEquals(
                "PENDING",
                saved.syncState
            )

            assertEquals(
                2L,
                applier.getCurrentCursor()
            )
        }

    @Test
    fun applyPage_shouldAdvanceCursorAndPreservePendingMovement() =
        runBlocking {

            database.movementDao().insertMovement(
                createLocalMovement(
                    id = "movement-pending",
                    quantity = 99.0,
                    pendingOp = "UPDATE",
                    syncState = "PENDING"
                )
            )

            val response = PullResponseDto(
                items = listOf(
                    createRemoteMovement(
                        id = "movement-new",
                        quantity = 5.0,
                        serverVersion = 1L,
                        serverSeq = 1L
                    ),
                    createRemoteMovement(
                        id = "movement-pending",
                        quantity = 20.0,
                        serverVersion = 2L,
                        serverSeq = 2L
                    )
                ),
                nextCursor = 2L,
                hasMore = false
            )

            applier.applyPage(response)

            val newMovement = database.movementDao()
                .getMovementById("movement-new")

            val pendingMovement = database.movementDao()
                .getMovementById("movement-pending")

            assertNotNull(newMovement)
            assertNotNull(pendingMovement)

            assertEquals(
                5.0,
                newMovement!!.quantity,
                0.0
            )

            assertEquals(
                99.0,
                pendingMovement!!.quantity,
                0.0
            )

            assertEquals(
                2L,
                applier.getCurrentCursor()
            )
        }

    @Test
    fun applyPage_shouldDeleteLocalMovementWhenServerSendsTombstone() =
        runBlocking {

            database.movementDao().insertMovement(
                createLocalMovement(
                    quantity = 10.0,
                    serverVersion = 1L
                )
            )

            val response = PullResponseDto(
                items = listOf(
                    createRemoteMovement(
                        quantity = 10.0,
                        serverVersion = 2L,
                        serverSeq = 2L,
                        isDeleted = true
                    )
                ),
                nextCursor = 2L,
                hasMore = false
            )

            applier.applyPage(response)

            val saved = database.movementDao()
                .getMovementById("movement-001")

            assertNull(saved)

            assertEquals(
                2L,
                applier.getCurrentCursor()
            )
        }

    @Test
    fun applyPage_shouldNotReplaceWithOlderServerVersion() =
        runBlocking {

            database.movementDao().insertMovement(
                createLocalMovement(
                    quantity = 25.0,
                    serverVersion = 5L
                )
            )

            val response = PullResponseDto(
                items = listOf(
                    createRemoteMovement(
                        quantity = 10.0,
                        serverVersion = 3L,
                        serverSeq = 6L
                    )
                ),
                nextCursor = 6L,
                hasMore = false
            )

            applier.applyPage(response)

            val saved = database.movementDao()
                .getMovementById("movement-001")

            assertNotNull(saved)

            assertEquals(
                25.0,
                saved!!.quantity,
                0.0
            )

            assertEquals(
                5L,
                saved.serverVersion
            )

            assertEquals(
                6L,
                applier.getCurrentCursor()
            )
        }
}