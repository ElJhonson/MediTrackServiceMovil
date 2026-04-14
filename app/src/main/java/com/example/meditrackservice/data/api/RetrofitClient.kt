package com.example.meditrackservice.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// data/api/RetrofitClient.kt
object RetrofitClient {

    const val BASE_URL = "http://10.34.0.171:8080/"

    fun create(
        tokenProvider: () -> String?,
        refreshTokenProvider: () -> String? = { null },
        onTokenRefreshed: (String) -> Unit = {},
        onSessionExpired: () -> Unit = {}
    ): ApiService {

        val client = OkHttpClient.Builder()
            .addInterceptor(
                AuthInterceptor(
                    tokenProvider = tokenProvider,
                    refreshTokenProvider = refreshTokenProvider,
                    onTokenRefreshed = onTokenRefreshed,
                    onSessionExpired = onSessionExpired
                )
            )
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}