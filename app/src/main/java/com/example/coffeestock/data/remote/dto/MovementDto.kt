package com.example.coffeestock.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PushRequestDto(
    val items: List<PushMovementDto>
)

data class PushMovementDto(
    val id: String,
    val operation: String,

    @SerializedName("mutation_id")
    val mutationId: String,

    @SerializedName("base_version")
    val baseVersion: Long,

    @SerializedName("local_rev")
    val localRev: Long,

    @SerializedName("product_id")
    val productId: String,

    @SerializedName("product_name")
    val productName: String,

    val type: String,
    val quantity: Double,

    @SerializedName("batch_code")
    val batchCode: String,

    val partner: String,
    val note: String,
    val status: String,

    @SerializedName("is_deleted")
    val isDeleted: Boolean,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String
)

data class PushResponseDto(
    val results: List<PushResultDto>
)

data class PushResultDto(
    val id: String,

    @SerializedName("mutation_id")
    val mutationId: String,

    val ok: Boolean,
    val movement: RemoteMovementDto?,
    val error: SyncErrorDto?
)

data class SyncErrorDto(
    val code: String,
    val message: String,

    @SerializedName("current_movement")
    val currentMovement: RemoteMovementDto?
)

data class PullResponseDto(
    val items: List<RemoteMovementDto>,

    @SerializedName("next_cursor")
    val nextCursor: Long?,

    @SerializedName("has_more")
    val hasMore: Boolean
)

data class RemoteMovementDto(
    val id: String,

    @SerializedName("product_id")
    val productId: String,

    @SerializedName("product_name")
    val productName: String,

    val type: String,
    val quantity: Double,

    @SerializedName("batch_code")
    val batchCode: String,

    val partner: String,
    val note: String,
    val status: String,

    @SerializedName("server_version")
    val serverVersion: Long,

    @SerializedName("server_seq")
    val serverSeq: Long,

    @SerializedName("is_deleted")
    val isDeleted: Boolean,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String
)