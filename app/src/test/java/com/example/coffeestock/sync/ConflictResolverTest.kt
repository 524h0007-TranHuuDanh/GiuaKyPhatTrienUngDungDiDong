package com.example.coffeestock.sync

import com.example.coffeestock.data.local.entity.MovementEntity
import com.example.coffeestock.data.remote.dto.RemoteMovementDto
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class ConflictResolverTest {

    private fun createLocalMovement(
        status: String = "DRAFT",
        isDeleted: Boolean = false
    ): MovementEntity {
        return MovementEntity(
            id = "movement-001",
            productId = "product-001",
            productName = "Ca phe Arabica",
            type = "IN",
            quantity = 10.0,
            batchCode = "BATCH-001",
            partner = "Nha cung cap A",
            note = "Local movement",
            status = status,
            serverVersion = 1L,
            syncState = "CONFLICT",
            pendingOp = if (isDeleted) "DELETE" else "UPDATE",
            localRev = 2L,
            mutationId = "mutation-local-001",
            isDeleted = isDeleted,
            createdAt = "2026-11-03T01:00:00.000Z",
            updatedAt = "2026-11-03T01:00:00.000Z"
        )
    }

    private fun createServerMovement(
        status: String = "DRAFT",
        isDeleted: Boolean = false,
        serverVersion: Long = 2L
    ): RemoteMovementDto {
        return RemoteMovementDto(
            id = "movement-001",
            productId = "product-001",
            productName = "Ca phe Arabica",
            type = "IN",
            quantity = 15.0,
            batchCode = "BATCH-001",
            partner = "Nha cung cap A",
            note = "Server movement",
            status = status,
            serverVersion = serverVersion,
            serverSeq = 10L,
            isDeleted = isDeleted,
            createdAt = "2026-11-03T01:00:00.000Z",
            updatedAt = "2026-11-03T02:00:00.000Z"
        )
    }

    @Test
    fun serverDeleted_shouldKeepServer() {
        val local = createLocalMovement()

        val server = createServerMovement(
            isDeleted = true,
            serverVersion = 2L
        )

        val decision = ConflictResolver.resolve(local, server)

        assertEquals(
            ConflictAction.KEEP_SERVER,
            decision.action
        )

        assertEquals(
            2L,
            decision.latestServerVersion
        )

        assertTrue(decision.message.isNotBlank())
    }

    @Test
    fun serverConfirmed_shouldKeepServer() {
        val local = createLocalMovement(
            status = "DRAFT"
        )

        val server = createServerMovement(
            status = "CONFIRMED",
            serverVersion = 3L
        )

        val decision = ConflictResolver.resolve(local, server)

        assertEquals(
            ConflictAction.KEEP_SERVER,
            decision.action
        )

        assertEquals(
            3L,
            decision.latestServerVersion
        )
    }

    @Test
    fun localDeleted_shouldRetryLocalDelete() {
        val local = createLocalMovement(
            isDeleted = true
        )

        val server = createServerMovement(
            serverVersion = 4L
        )

        val decision = ConflictResolver.resolve(local, server)

        assertEquals(
            ConflictAction.RETRY_LOCAL_DELETE,
            decision.action
        )

        assertEquals(
            4L,
            decision.latestServerVersion
        )
    }

    @Test
    fun localConfirmed_shouldRetryLocal() {
        val local = createLocalMovement(
            status = "CONFIRMED"
        )

        val server = createServerMovement(
            status = "DRAFT",
            serverVersion = 5L
        )

        val decision = ConflictResolver.resolve(local, server)

        assertEquals(
            ConflictAction.RETRY_LOCAL,
            decision.action
        )

        assertEquals(
            5L,
            decision.latestServerVersion
        )
    }

    @Test
    fun bothDraft_shouldRetryLocal() {
        val local = createLocalMovement(
            status = "DRAFT"
        )

        val server = createServerMovement(
            status = "DRAFT",
            serverVersion = 6L
        )

        val decision = ConflictResolver.resolve(local, server)

        assertEquals(
            ConflictAction.RETRY_LOCAL,
            decision.action
        )

        assertEquals(
            6L,
            decision.latestServerVersion
        )
    }

    @Test
    fun serverConfirmedAndLocalDeleted_shouldKeepServer() {
        // Quy tắc ưu tiên của ConflictResolver:
        // server CONFIRMED được kiểm tra trước local.isDeleted.

        val local = createLocalMovement(
            status = "DRAFT",
            isDeleted = true
        )

        val server = createServerMovement(
            status = "CONFIRMED",
            serverVersion = 7L
        )

        val decision = ConflictResolver.resolve(local, server)

        assertEquals(
            ConflictAction.KEEP_SERVER,
            decision.action
        )

        assertEquals(
            7L,
            decision.latestServerVersion
        )
    }
}