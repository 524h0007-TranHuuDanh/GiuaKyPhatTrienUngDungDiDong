package com.example.coffeestock.data.remote

import com.example.coffeestock.data.remote.dto.PullResponseDto
import com.example.coffeestock.data.remote.dto.PushRequestDto
import com.example.coffeestock.data.remote.dto.PushResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @POST("api/v1/sync/push")
    suspend fun pushChanges(
        @Header("X-API-Key") apiKey: String,
        @Body request: PushRequestDto
    ): Response<PushResponseDto>

    @GET("api/v1/sync/pull")
    suspend fun pullChanges(
        @Header("X-API-Key") apiKey: String,
        @Query("after_seq") afterSeq: Long,
        @Query("limit") limit: Int
    ): Response<PullResponseDto>
}