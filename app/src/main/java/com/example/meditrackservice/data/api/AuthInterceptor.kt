package com.example.meditrackservice.data.api

import android.util.Log
import com.example.meditrackservice.data.model.RefreshResponse
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

// data/api/AuthInterceptor.kt
class AuthInterceptor(
    private val tokenProvider: () -> String?,
    private val refreshTokenProvider: () -> String?,
    private val onTokenRefreshed: (String) -> Unit,
    private val onSessionExpired: () -> Unit
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()

        // Hacer la petición original con el token actual
        val request = buildRequest(chain.request(), token)
        val response = chain.proceed(request)

        // Si responde 401 → intentar refresh
        if (response.code == 401) {
            response.close()

            val refreshToken = refreshTokenProvider() ?: run {
                onSessionExpired()
                return response
            }

            // Llamar al endpoint de refresh
            val newToken = intentarRefresh(chain, refreshToken)

            return if (newToken != null) {
                onTokenRefreshed(newToken)
                // Reintentar la petición original con el nuevo token
                val newRequest = buildRequest(chain.request(), newToken)
                chain.proceed(newRequest)
            } else {
                onSessionExpired()
                response
            }
        }

        return response
    }

    private fun buildRequest(request: Request, token: String?): Request {
        return if (token != null) {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }
    }

    private fun intentarRefresh(chain: Interceptor.Chain, refreshToken: String): String? {
        return try {
            val refreshBody = """{"refreshToken":"$refreshToken"}"""
                .toRequestBody("application/json".toMediaType())

            val refreshRequest = Request.Builder()
                .url("${RetrofitClient.BASE_URL}auth/refresh")
                .post(refreshBody)
                .build()

            val refreshResponse = chain.proceed(refreshRequest)

            if (refreshResponse.isSuccessful) {
                val body = refreshResponse.body?.string()
                refreshResponse.close()
                // Extraer el accessToken del JSON
                Gson().fromJson(body, RefreshResponse::class.java).accessToken
            } else {
                refreshResponse.close()
                null
            }
        } catch (e: Exception) {
            Log.e("AuthInterceptor", "Error en refresh: ${e.message}")
            null
        }
    }
}