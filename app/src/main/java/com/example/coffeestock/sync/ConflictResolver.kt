package com.example.coffeestock.sync

import com.example.coffeestock.data.local.entity.MovementEntity
import com.example.coffeestock.data.remote.dto.RemoteMovementDto

enum class ConflictAction {
    KEEP_SERVER,
    RETRY_LOCAL,
    RETRY_LOCAL_DELETE
}

data class ConflictDecision(
    val action: ConflictAction,
    val latestServerVersion: Long,
    val message: String
)

object ConflictResolver {

    fun resolve(
        local: MovementEntity,
        server: RemoteMovementDto
    ): ConflictDecision {

        // Máy chủ đã xóa: không khôi phục lại phiếu từ local.
        if (server.isDeleted) {
            return ConflictDecision(
                action = ConflictAction.KEEP_SERVER,
                latestServerVersion = server.serverVersion,
                message = "Máy chủ đã xóa phiếu. Giữ trạng thái đã xóa."
            )
        }

        // Phiếu đã xác nhận trên server: không ghi đè bằng bản nháp local.
        if (server.status == "CONFIRMED") {
            return ConflictDecision(
                action = ConflictAction.KEEP_SERVER,
                latestServerVersion = server.serverVersion,
                message = "Phiếu trên máy chủ đã xác nhận."
            )
        }

        // Local đã yêu cầu xóa: gửi lại yêu cầu xóa với version mới.
        if (local.isDeleted || local.pendingOp == "DELETE") {
            return ConflictDecision(
                action = ConflictAction.RETRY_LOCAL_DELETE,
                latestServerVersion = server.serverVersion,
                message = "Giữ yêu cầu xóa ở local và gửi lại."
            )
        }

        // Local đã xác nhận: giữ bản local và thử gửi lại.
        if (local.status == "CONFIRMED") {
            return ConflictDecision(
                action = ConflictAction.RETRY_LOCAL,
                latestServerVersion = server.serverVersion,
                message = "Giữ phiếu đã xác nhận ở local và gửi lại."
            )
        }

        // Trường hợp còn lại: giữ thay đổi local.
        return ConflictDecision(
            action = ConflictAction.RETRY_LOCAL,
            latestServerVersion = server.serverVersion,
            message = "Giữ thay đổi local và gửi lại với serverVersion mới."
        )
    }
}