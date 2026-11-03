package com.example.coffeestock.data.remote

import com.example.coffeestock.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private val baseUrl: String = BuildConfig.API_BASE_URL.let { url ->
        if (url.endsWith("/")) {
            url
        } else {
            "$url/"
        }
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(ApiService::class.java)
    }
}